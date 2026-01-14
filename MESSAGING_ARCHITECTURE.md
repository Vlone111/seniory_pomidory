# Архитектура Брокера Сообщений (RabbitMQ) и Интеграция Платежей

## Текущая Архитектура RabbitMQ

### Обзор

В репозитории используется **RabbitMQ** в качестве брокера сообщений для асинхронной коммуникации между микросервисами. Основная цель - обеспечить слабую связанность (loose coupling) между сервисами и надежную доставку событий.

### Компоненты

#### 1. RabbitMQ Сервер
```yaml
# docker-compose.yml
rabbitmq:
  image: rabbitmq:3-management
  ports:
    - "15672:15672"  # Management UI
    - "5672:5672"    # AMQP порт
    - "15692:15692"  # Prometheus metrics
  environment:
    - RABBITMQ_DEFAULT_USER=admin
    - RABBITMQ_DEFAULT_PASS=admin
```

**Доступ к UI:** http://localhost:15672 (admin/admin)

### Текущая Схема Обмена Сообщениями

```
┌─────────────────────────────────────────────────────────────────┐
│                      ORDER EXCHANGE                              │
│                    (order-exchange)                              │
│                      Type: Topic                                 │
└─────────────────────────────────────────────────────────────────┘
           │                                    │
           │                                    │
           ▼                                    ▼
   ┌───────────────────┐              ┌──────────────────┐
   │ stock.decrease    │              │ order.completed  │
   │     Queue         │              │      Queue       │
   └───────────────────┘              └──────────────────┘
           │                                    │
           │                                    │
           ▼                                    ▼
   ┌───────────────────┐              ┌──────────────────┐
   │  ProductService   │              │   CartService    │
   │  (Consumer)       │              │   (Consumer)     │
   └───────────────────┘              └──────────────────┘
```

### Поток Событий

#### 1. Создание Заказа → Уменьшение Стока

**Publisher:** [`OrderService`](orderservice/src/main/java/com/marketplace/orderservice/service/OrderService.java:134-150)

```java
// OrderService публикует событие
private void publishStockDecreaseEvents(List<OrderItemRequestDto> items) {
    for (OrderItemRequestDto item : items) {
        StockDecreaseEvent event = StockDecreaseEvent.builder()
            .productId(item.getProductId())
            .sizeId(null)
            .quantity(item.getQuantity())
            .build();
            
        rabbitTemplate.convertAndSend(orderExchange, stockDecreaseRoutingKey, event);
    }
}
```

**Consumer:** [`ProductService`](productservice/src/main/java/com/marketplace/productservice/messaging/StockDecreaseListener.java:17-28)

```java
@RabbitListener(queues = "${rabbitmq.queue.stock-decrease}")
public void handleStockDecrease(StockDecreaseDto stockDecreaseDto) {
    productSizeService.decreaseStock(stockDecreaseDto);
}
```

**Настройки:**
- Exchange: `order-exchange`
- Queue: `stock.decrease`
- Routing Key: `stock.decrease`

#### 2. Завершение Заказа → Очистка Корзины

**Publisher:** [`OrderService`](orderservice/src/main/java/com/marketplace/orderservice/service/OrderService.java:153-167)

```java
private void publishOrderCompletedEvent(UUID userId, UUID orderId) {
    OrderCompletedEvent event = OrderCompletedEvent.builder()
        .userId(userId)
        .orderId(orderId)
        .build();
        
    rabbitTemplate.convertAndSend(orderExchange, orderCompletedRoutingKey, event);
}
```

**Consumer:** [`CartService`](cartservice/src/main/java/com/marketplace/cartservice/messaging/OrderCompletedListener.java:17-29)

```java
@RabbitListener(queues = "${rabbitmq.queue.order-completed}")
public void handleOrderCompleted(OrderCompletedEvent event) {
    cartService.clearCart(event.getUserId());
}
```

**Настройки:**
- Exchange: `order-exchange`
- Queue: `order.completed`
- Routing Key: `order.completed`

### Конфигурация RabbitMQ

#### OrderService

[`RabbitMQConfig.java`](orderservice/src/main/java/com/marketplace/orderservice/config/RabbitMQConfig.java:1-72)

```java
@Configuration
public class RabbitMQConfig {
    
    // Создание Topic Exchange
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange);
    }
    
    // Создание очередей
    @Bean
    public Queue stockDecreaseQueue() {
        return QueueBuilder.durable(stockDecreaseQueue).build();
    }
    
    @Bean
    public Queue orderCompletedQueue() {
        return QueueBuilder.durable(orderCompletedQueue).build();
    }
    
    // Связывание очередей с exchange
    @Bean
    public Binding stockDecreaseBinding() {
        return BindingBuilder
            .bind(stockDecreaseQueue())
            .to(orderExchange())
            .with(stockDecreaseRoutingKey);
    }
    
    // JSON конвертер для сообщений
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

[`application.yml`](orderservice/src/main/resources/application.yml:26-48)

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    listener:
      simple:
        acknowledge-mode: auto
        retry:
          enabled: true
          initial-interval: 3000
          max-attempts: 3
          multiplier: 2

rabbitmq:
  exchange:
    order: order-exchange
  queue:
    stock-decrease: stock.decrease
    order-completed: order.completed
  routing-key:
    stock-decrease: stock.decrease
    order-completed: order.completed
```

### Преимущества Текущей Архитектуры

1. **Асинхронность**: Сервисы не блокируют друг друга
2. **Надежность**: RabbitMQ гарантирует доставку сообщений
3. **Масштабируемость**: Можно добавлять consumer'ов
4. **Повторные попытки**: Автоматический retry при ошибках
5. **Слабая связанность**: Сервисы не знают друг о друге напрямую

---

## Интеграция CDEK Доставки

### Архитектура Интеграции

```
┌──────────────┐      ┌──────────────────┐      ┌──────────────┐
│ OrderService │─────▶│ DeliveryService  │─────▶│  CDEK API    │
└──────────────┘      └──────────────────┘      └──────────────┘
       │                       │
       │                       │
       ▼                       ▼
┌──────────────────────────────────────────┐
│         RabbitMQ Exchange                │
│   delivery-exchange (Topic)              │
└──────────────────────────────────────────┘
       │                       │
       ▼                       ▼
┌──────────────┐      ┌──────────────────┐
│ delivery.    │      │ delivery.status  │
│ calculate    │      │ .updated         │
└──────────────┘      └──────────────────┘
```

### Шаг 1: Создание DeliveryService

#### 1.1 Структура проекта

```
deliveryservice/
├── src/main/java/com/marketplace/deliveryservice/
│   ├── DeliveryServiceApplication.java
│   ├── config/
│   │   ├── RabbitMQConfig.java
│   │   └── CdekConfig.java
│   ├── client/
│   │   └── CdekApiClient.java
│   ├── controller/
│   │   └── DeliveryController.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CalculateDeliveryRequest.java
│   │   │   └── CreateDeliveryRequest.java
│   │   └── response/
│   │       ├── DeliveryCalculationResponse.java
│   │       └── DeliveryTrackingResponse.java
│   ├── entity/
│   │   └── Delivery.java
│   ├── event/
│   │   ├── DeliveryCalculateEvent.java
│   │   ├── DeliveryCreatedEvent.java
│   │   └── DeliveryStatusUpdatedEvent.java
│   ├── messaging/
│   │   └── DeliveryEventListener.java
│   ├── repository/
│   │   └── DeliveryRepository.java
│   └── service/
│       └── DeliveryService.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__create_delivery_table.sql
├── Dockerfile
└── pom.xml
```

#### 1.2 Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- RabbitMQ -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
    
    <!-- REST Client для CDEK API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

#### 1.3 CDEK API Client

```java
package com.marketplace.deliveryservice.client;

import com.marketplace.deliveryservice.dto.cdek.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class CdekApiClient {
    
    @Value("${cdek.api.url}")
    private String cdekApiUrl;
    
    @Value("${cdek.api.account}")
    private String account;
    
    @Value("${cdek.api.secure-password}")
    private String securePassword;
    
    private final WebClient.Builder webClientBuilder;
    
    private String accessToken;
    
    // Получение токена доступа
    public String getAccessToken() {
        if (accessToken == null) {
            log.info("Requesting CDEK access token");
            
            String credentials = account + ":" + securePassword;
            String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes());
            
            CdekTokenResponse response = webClientBuilder.build()
                .post()
                .uri(cdekApiUrl + "/v2/oauth/token?grant_type=client_credentials")
                .header("Authorization", "Basic " + encodedCredentials)
                .retrieve()
                .bodyToMono(CdekTokenResponse.class)
                .block();
            
            accessToken = response.getAccessToken();
            log.info("CDEK access token obtained successfully");
        }
        return accessToken;
    }
    
    // Расчет стоимости доставки
    public CdekCalculationResponse calculateDelivery(CdekCalculationRequest request) {
        log.info("Calculating delivery cost for tariff: {}", request.getTariffCode());
        
        return webClientBuilder.build()
            .post()
            .uri(cdekApiUrl + "/v2/calculator/tariff")
            .header("Authorization", "Bearer " + getAccessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CdekCalculationResponse.class)
            .block();
    }
    
    // Создание заказа на доставку
    public CdekOrderResponse createOrder(CdekOrderRequest request) {
        log.info("Creating CDEK delivery order");
        
        return webClientBuilder.build()
            .post()
            .uri(cdekApiUrl + "/v2/orders")
            .header("Authorization", "Bearer " + getAccessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CdekOrderResponse.class)
            .block();
    }
    
    // Получение статуса заказа
    public CdekOrderStatusResponse getOrderStatus(String cdekOrderUuid) {
        log.info("Getting CDEK order status for: {}", cdekOrderUuid);
        
        return webClientBuilder.build()
            .get()
            .uri(cdekApiUrl + "/v2/orders/" + cdekOrderUuid)
            .header("Authorization", "Bearer " + getAccessToken())
            .retrieve()
            .bodyToMono(CdekOrderStatusResponse.class)
            .block();
    }
    
    // Получение списка ПВЗ (пунктов выдачи заказов)
    public CdekPickupPointsResponse getPickupPoints(String cityCode) {
        log.info("Getting pickup points for city: {}", cityCode);
        
        return webClientBuilder.build()
            .get()
            .uri(cdekApiUrl + "/v2/deliverypoints?city_code=" + cityCode)
            .header("Authorization", "Bearer " + getAccessToken())
            .retrieve()
            .bodyToMono(CdekPickupPointsResponse.class)
            .block();
    }
}
```

#### 1.4 RabbitMQ Configuration

```java
package com.marketplace.deliveryservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    @Value("${rabbitmq.exchange.delivery}")
    private String deliveryExchange;
    
    @Value("${rabbitmq.queue.delivery-calculate}")
    private String deliveryCalculateQueue;
    
    @Value("${rabbitmq.queue.delivery-create}")
    private String deliveryCreateQueue;
    
    @Value("${rabbitmq.queue.delivery-status-updated}")
    private String deliveryStatusUpdatedQueue;
    
    @Value("${rabbitmq.routing-key.delivery-calculate}")
    private String deliveryCalculateRoutingKey;
    
    @Value("${rabbitmq.routing-key.delivery-create}")
    private String deliveryCreateRoutingKey;
    
    @Value("${rabbitmq.routing-key.delivery-status-updated}")
    private String deliveryStatusUpdatedRoutingKey;
    
    @Bean
    public TopicExchange deliveryExchange() {
        return new TopicExchange(deliveryExchange);
    }
    
    @Bean
    public Queue deliveryCalculateQueue() {
        return QueueBuilder.durable(deliveryCalculateQueue).build();
    }
    
    @Bean
    public Queue deliveryCreateQueue() {
        return QueueBuilder.durable(deliveryCreateQueue).build();
    }
    
    @Bean
    public Queue deliveryStatusUpdatedQueue() {
        return QueueBuilder.durable(deliveryStatusUpdatedQueue).build();
    }
    
    @Bean
    public Binding deliveryCalculateBinding() {
        return BindingBuilder
            .bind(deliveryCalculateQueue())
            .to(deliveryExchange())
            .with(deliveryCalculateRoutingKey);
    }
    
    @Bean
    public Binding deliveryCreateBinding() {
        return BindingBuilder
            .bind(deliveryCreateQueue())
            .to(deliveryExchange())
            .with(deliveryCreateRoutingKey);
    }
    
    @Bean
    public Binding deliveryStatusUpdatedBinding() {
        return BindingBuilder
            .bind(deliveryStatusUpdatedQueue())
            .to(deliveryExchange())
            .with(deliveryStatusUpdatedRoutingKey);
    }
    
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
```

#### 1.5 Delivery Service

```java
package com.marketplace.deliveryservice.service;

import com.marketplace.deliveryservice.client.CdekApiClient;
import com.marketplace.deliveryservice.dto.cdek.*;
import com.marketplace.deliveryservice.entity.Delivery;
import com.marketplace.deliveryservice.entity.DeliveryStatus;
import com.marketplace.deliveryservice.event.DeliveryCreatedEvent;
import com.marketplace.deliveryservice.event.DeliveryStatusUpdatedEvent;
import com.marketplace.deliveryservice.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {
    
    private final DeliveryRepository deliveryRepository;
    private final CdekApiClient cdekApiClient;
    private final RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.exchange.delivery}")
    private String deliveryExchange;
    
    @Value("${rabbitmq.routing-key.delivery-status-updated}")
    private String deliveryStatusUpdatedRoutingKey;
    
    // Расчет стоимости доставки
    public BigDecimal calculateDeliveryCost(CalculateDeliveryRequest request) {
        log.info("Calculating delivery cost from {} to {}", 
            request.getFromCityCode(), request.getToCityCode());
        
        CdekCalculationRequest cdekRequest = CdekCalculationRequest.builder()
            .tariffCode(request.getTariffCode())
            .fromLocation(CdekLocation.builder()
                .code(request.getFromCityCode())
                .build())
            .toLocation(CdekLocation.builder()
                .code(request.getToCityCode())
                .postalCode(request.getToPostalCode())
                .build())
            .packages(request.getPackages())
            .build();
        
        CdekCalculationResponse response = cdekApiClient.calculateDelivery(cdekRequest);
        
        log.info("Delivery cost calculated: {} RUB, period: {} days",
            response.getTotalSum(), response.getPeriodMax());
        
        return response.getTotalSum();
    }
    
    // Создание заказа на доставку
    @Transactional
    public Delivery createDelivery(UUID orderId, CreateDeliveryRequest request) {
        log.info("Creating delivery for order: {}", orderId);
        
        // Создание заказа в CDEK
        CdekOrderRequest cdekRequest = buildCdekOrderRequest(request);
        CdekOrderResponse cdekResponse = cdekApiClient.createOrder(cdekRequest);
        
        // Сохранение в БД
        Delivery delivery = Delivery.builder()
            .orderId(orderId)
            .cdekOrderUuid(cdekResponse.getEntity().getUuid())
            .trackingNumber(cdekResponse.getEntity().getCdekNumber())
            .status(DeliveryStatus.CREATED)
            .tariffCode(request.getTariffCode())
            .fromCityCode(request.getFromCityCode())
            .toCityCode(request.getToCityCode())
            .recipientName(request.getRecipientName())
            .recipientPhone(request.getRecipientPhone())
            .deliveryCost(cdekResponse.getEntity().getDeliveryCost())
            .build();
        
        delivery = deliveryRepository.save(delivery);
        
        // Публикация события
        publishDeliveryCreatedEvent(delivery);
        
        return delivery;
    }
    
    // Обновление статуса доставки
    @Transactional
    public void updateDeliveryStatus(UUID deliveryId) {
        log.info("Updating delivery status for: {}", deliveryId);
        
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new RuntimeException("Delivery not found"));
        
        CdekOrderStatusResponse status = cdekApiClient
            .getOrderStatus(delivery.getCdekOrderUuid());
        
        DeliveryStatus oldStatus = delivery.getStatus();
        DeliveryStatus newStatus = mapCdekStatusToDeliveryStatus(status.getStatus());
        
        if (!oldStatus.equals(newStatus)) {
            delivery.setStatus(newStatus);
            deliveryRepository.save(delivery);
            
            // Публикация события об изменении статуса
            publishDeliveryStatusUpdatedEvent(delivery, oldStatus, newStatus);
        }
    }
    
    private void publishDeliveryCreatedEvent(Delivery delivery) {
        DeliveryCreatedEvent event = DeliveryCreatedEvent.builder()
            .deliveryId(delivery.getId())
            .orderId(delivery.getOrderId())
            .trackingNumber(delivery.getTrackingNumber())
            .build();
        
        rabbitTemplate.convertAndSend(
            deliveryExchange,
            "delivery.created",
            event
        );
    }
    
    private void publishDeliveryStatusUpdatedEvent(
            Delivery delivery,
            DeliveryStatus oldStatus,
            DeliveryStatus newStatus) {
        
        DeliveryStatusUpdatedEvent event = DeliveryStatusUpdatedEvent.builder()
            .deliveryId(delivery.getId())
            .orderId(delivery.getOrderId())
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .trackingNumber(delivery.getTrackingNumber())
            .build();
        
        rabbitTemplate.convertAndSend(
            deliveryExchange,
            deliveryStatusUpdatedRoutingKey,
            event
        );
    }
    
    private CdekOrderRequest buildCdekOrderRequest(CreateDeliveryRequest request) {
        // Построение запроса к CDEK API
        return CdekOrderRequest.builder()
            .tariffCode(request.getTariffCode())
            .sender(buildSender(request))
            .recipient(buildRecipient(request))
            .packages(request.getPackages())
            .build();
    }
    
    private DeliveryStatus mapCdekStatusToDeliveryStatus(String cdekStatus) {
        // Маппинг статусов CDEK на внутренние статусы
        return switch (cdekStatus) {
            case "CREATED" -> DeliveryStatus.CREATED;
            case "ACCEPTED" -> DeliveryStatus.ACCEPTED;
            case "IN_TRANSIT" -> DeliveryStatus.IN_TRANSIT;
            case "DELIVERED" -> DeliveryStatus.DELIVERED;
            case "CANCELED" -> DeliveryStatus.CANCELED;
            default -> DeliveryStatus.UNKNOWN;
        };
    }
}
```

#### 1.6 Application Configuration

```yaml
spring:
  application:
    name: delivery-service
  
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:deliverydb}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  
  flyway:
    enabled: true
    baseline-on-migrate: true
  
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}

# CDEK API Configuration
cdek:
  api:
    url: https://api.cdek.ru
    account: ${CDEK_ACCOUNT}
    secure-password: ${CDEK_SECURE_PASSWORD}

# RabbitMQ Configuration
rabbitmq:
  exchange:
    delivery: delivery-exchange
  queue:
    delivery-calculate: delivery.calculate
    delivery-create: delivery.create
    delivery-status-updated: delivery.status.updated
  routing-key:
    delivery-calculate: delivery.calculate
    delivery-create: delivery.create
    delivery-status-updated: delivery.status.updated

server:
  port: ${SERVER_PORT:8090}
```

---

## Интеграция YooKassa (Оплата)

### Архитектура Интеграции

```
┌──────────────┐      ┌──────────────────┐      ┌──────────────┐
│ OrderService │─────▶│ PaymentService   │─────▶│ YooKassa API │
└──────────────┘      └──────────────────┘      └──────────────┘
       │                       │
       │                       │
       ▼                       ▼
┌──────────────────────────────────────────┐
│         RabbitMQ Exchange                │
│   payment-exchange (Topic)               │
└──────────────────────────────────────────┘
       │                       │
       ▼                       ▼
┌──────────────┐      ┌──────────────────┐
│ payment.     │      │ payment.status   │
│ create       │      │ .updated         │
└──────────────┘      └──────────────────┘
       │                       │
       ▼                       ▼
┌──────────────┐      ┌──────────────────┐
│PaymentService│      │  OrderService    │
│(Consumer)    │      │  (Consumer)      │
└──────────────┘      └──────────────────┘
```

### Шаг 1: Создание PaymentService

#### 1.1 Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- RabbitMQ -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
    
    <!-- YooKassa SDK -->
    <dependency>
        <groupId>com.yookassa</groupId>
        <artifactId>yookassa-sdk-java</artifactId>
        <version>2.0.0</version>
    </dependency>
    
    <!-- WebFlux для HTTP клиента -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

#### 1.2 YooKassa Client

```java
package com.marketplace.paymentservice.client;

import com.marketplace.paymentservice.dto.yookassa.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class YooKassaClient {
    
    @Value("${yookassa.api.url}")
    private String yookassaApiUrl;
    
    @Value("${yookassa.api.shop-id}")
    private String shopId;
    
    @Value("${yookassa.api.secret-key}")
    private String secretKey;
    
    private final WebClient.Builder webClientBuilder;
    
    // Создание платежа
    public YooKassaPaymentResponse createPayment(YooKassaPaymentRequest request) {
        log.info("Creating YooKassa payment for amount: {}", request.getAmount().getValue());
        
        String auth = Base64.getEncoder()
            .encodeToString((shopId + ":" + secretKey).getBytes());
        
        String idempotenceKey = UUID.randomUUID().toString();
        
        return webClientBuilder.build()
            .post()
            .uri(yookassaApiUrl + "/v3/payments")
            .header("Authorization", "Basic " + auth)
            .header("Idempotence-Key", idempotenceKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(YooKassaPaymentResponse.class)
            .block();
    }
    
    // Получение информации о платеже
    public YooKassaPaymentResponse getPayment(String paymentId) {
        log.info("Getting payment info for: {}", paymentId);
        
        String auth = Base64.getEncoder()
            .encodeToString((shopId + ":" + secretKey).getBytes());
        
        return webClientBuilder.build()
            .get()
            .uri(yookassaApiUrl + "/v3/payments/" + paymentId)
            .header("Authorization", "Basic " + auth)
            .retrieve()
            .bodyToMono(YooKassaPaymentResponse.class)
            .block();
    }
    
    // Захват платежа (для двухстадийных платежей)
    public YooKassaPaymentResponse capturePayment(String paymentId, YooKassaCaptureRequest request) {
        log.info("Capturing payment: {}", paymentId);
        
        String auth = Base64.getEncoder()
            .encodeToString((shopId + ":" + secretKey).getBytes());
        
        String idempotenceKey = UUID.randomUUID().toString();
        
        return webClientBuilder.build()
            .post()
            .uri(yookassaApiUrl + "/v3/payments/" + paymentId + "/capture")
            .header("Authorization", "Basic " + auth)
            .header("Idempotence-Key", idempotenceKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(YooKassaPaymentResponse.class)
            .block();
    }
    
    // Отмена платежа
    public YooKassaPaymentResponse cancelPayment(String paymentId) {
        log.info("Canceling payment: {}", paymentId);
        
        String auth = Base64.getEncoder()
            .encodeToString((shopId + ":" + secretKey).getBytes());
        
        String idempotenceKey = UUID.randomUUID().toString();
        
        return webClientBuilder.build()
            .post()
            .uri(yookassaApiUrl + "/v3/payments/" + paymentId + "/cancel")
            .header("Authorization", "Basic " + auth)
            .header("Idempotence-Key", idempotenceKey)
            .retrieve()
            .bodyToMono(YooKassaPaymentResponse.class)
            .block();
    }
    
    // Создание возврата
    public YooKassaRefundResponse createRefund(YooKassaRefundRequest request) {
        log.info("Creating refund for payment: {}", request.getPaymentId());
        
        String auth = Base64.getEncoder()
            .encodeToString((shopId + ":" + secretKey).getBytes());
        
        String idempotenceKey = UUID.randomUUID().toString();
        
        return webClientBuilder.build()
            .post()
            .uri(yookassaApiUrl + "/v3/refunds")
            .header("Authorization", "Basic " + auth)
            .header("Idempotence-Key", idempotenceKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(YooKassaRefundResponse.class)
            .block();
    }
}
```

#### 1.3 Payment Service

```java
package com.marketplace.paymentservice.service;

import com.marketplace.paymentservice.client.YooKassaClient;
import com.marketplace.paymentservice.dto.request.CreatePaymentRequest;
import com.marketplace.paymentservice.dto.yookassa.*;
import com.marketplace.paymentservice.entity.Payment;
import com.marketplace.paymentservice.entity.PaymentStatus;
import com.marketplace.paymentservice.event.PaymentCreatedEvent;
import com.marketplace.paymentservice.event.PaymentStatusUpdatedEvent;
import com.marketplace.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final YooKassaClient yooKassaClient;
    private final RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.exchange.payment}")
    private String paymentExchange;
    
    @Value("${yookassa.return-url}")
    private String returnUrl;
    
    // Создание платежа
    @Transactional
    public Payment createPayment(UUID orderId, CreatePaymentRequest request) {
        log.info("Creating payment for order: {}", orderId);
        
        // Создание платежа в YooKassa
        YooKassaPaymentRequest yookassaRequest = YooKassaPaymentRequest.builder()
            .amount(YooKassaAmount.builder()
                .value(request.getAmount().toString())
                .currency("RUB")
                .build())
            .confirmation(YooKassaConfirmation.builder()
                .type("redirect")
                .returnUrl(returnUrl + "?orderId=" + orderId)
                .build())
            .description("Оплата заказа #" + orderId)
            .metadata(YooKassaMetadata.builder()
                .orderId(orderId.toString())
                .build())
            .capture(true) // Автоматический захват платежа
            .build();
        
        YooKassaPaymentResponse response = yooKassaClient.createPayment(yookassaRequest);
        
        // Сохранение в БД
        Payment payment = Payment.builder()
            .orderId(orderId)
            .yookassaPaymentId(response.getId())
            .amount(request.getAmount())
            .status(mapYooKassaStatusToPaymentStatus(response.getStatus()))
            .confirmationUrl(response.getConfirmation().getConfirmationUrl())
            .build();
        
        payment = paymentRepository.save(payment);
        
        // Публикация события
        publishPaymentCreatedEvent(payment);
        
        return payment;
    }
    
    // Обработка webhook от YooKassa
    @Transactional
    public void handleYooKassaWebhook(YooKassaWebhookRequest webhook) {
        log.info("Processing YooKassa webhook: {}", webhook.getEvent());
        
        if ("payment.succeeded".equals(webhook.getEvent()) ||
            "payment.canceled".equals(webhook.getEvent())) {
            
            YooKassaPaymentResponse paymentData = webhook.getObject();
            
            Payment payment = paymentRepository
                .findByYookassaPaymentId(paymentData.getId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));
            
            PaymentStatus oldStatus = payment.getStatus();
            PaymentStatus newStatus = mapYooKassaStatusToPaymentStatus(paymentData.getStatus());
            
            if (!oldStatus.equals(newStatus)) {
                payment.setStatus(newStatus);
                paymentRepository.save(payment);
                
                // Публикация события об изменении статуса
                publishPaymentStatusUpdatedEvent(payment, oldStatus, newStatus);
            }
        }
    }
    
    // Возврат средств
    @Transactional
    public void refundPayment(UUID paymentId, BigDecimal amount) {
        log.info("Processing refund for payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        YooKassaRefundRequest refundRequest = YooKassaRefundRequest.builder()
            .paymentId(payment.getYookassaPaymentId())
            .amount(YooKassaAmount.builder()
                .value(amount.toString())
                .currency("RUB")
                .build())
            .build();
        
        yooKassaClient.createRefund(refundRequest);
        
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
    }
    
    private void publishPaymentCreatedEvent(Payment payment) {
        PaymentCreatedEvent event = PaymentCreatedEvent.builder()
            .paymentId(payment.getId())
            .orderId(payment.getOrderId())
            .amount(payment.getAmount())
            .confirmationUrl(payment.getConfirmationUrl())
            .build();
        
        rabbitTemplate.convertAndSend(
            paymentExchange,
            "payment.created",
            event
        );
    }
    
    private void publishPaymentStatusUpdatedEvent(
            Payment payment,
            PaymentStatus oldStatus,
            PaymentStatus newStatus) {
        
        PaymentStatusUpdatedEvent event = PaymentStatusUpdatedEvent.builder()
            .paymentId(payment.getId())
            .orderId(payment.getOrderId())
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .amount(payment.getAmount())
            .build();
        
        rabbitTemplate.convertAndSend(
            paymentExchange,
            "payment.status.updated",
            event
        );
    }
    
    private PaymentStatus mapYooKassaStatusToPaymentStatus(String yookassaStatus) {
        return switch (yookassaStatus) {
            case "pending" -> PaymentStatus.PENDING;
            case "waiting_for_capture" -> PaymentStatus.WAITING_FOR_CAPTURE;
            case "succeeded" -> PaymentStatus.SUCCEEDED;
            case "canceled" -> PaymentStatus.CANCELED;
            default -> PaymentStatus.UNKNOWN;
        };
    }
}
```

#### 1.4 Webhook Controller

```java
package com.marketplace.paymentservice.controller;

import com.marketplace.paymentservice.dto.yookassa.YooKassaWebhookRequest;
import com.marketplace.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/webhooks")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/yookassa")
    public ResponseEntity<Void> handleYooKassaWebhook(
            @RequestBody YooKassaWebhookRequest webhook) {
        
        log.info("Received YooKassa webhook: {}", webhook.getEvent());
        
        try {
            paymentService.handleYooKassaWebhook(webhook);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
```

#### 1.5 Application Configuration

```yaml
spring:
  application:
    name: payment-service
  
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:paymentdb}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  
  flyway:
    enabled: true
    baseline-on-migrate: true
  
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}

# YooKassa Configuration
yookassa:
  api:
    url: https://api.yookassa.ru
    shop-id: ${YOOKASSA_SHOP_ID}
    secret-key: ${YOOKASSA_SECRET_KEY}
  return-url: ${YOOKASSA_RETURN_URL:http://localhost:3000/payment/return}

# RabbitMQ Configuration
rabbitmq:
  exchange:
    payment: payment-exchange
  queue:
    payment-create: payment.create
    payment-status-updated: payment.status.updated
  routing-key:
    payment-create: payment.create
    payment-status-updated: payment.status.updated

server:
  port: ${SERVER_PORT:8091}
```

---

## Интеграция в OrderService

### Обновление OrderService для работы с новыми сервисами

#### 1. Добавление событий

```java
// Event для запроса расчета доставки
package com.marketplace.orderservice.event;

@Data
@Builder
public class DeliveryCalculateRequestEvent implements Serializable {
    private UUID orderId;
    private String fromCityCode;
    private String toCityCode;
    private String toPostalCode;
    private Integer tariffCode;
    private List<PackageInfo> packages;
}

// Event для создания доставки
@Data
@Builder
public class DeliveryCreateRequestEvent implements Serializable {
    private UUID orderId;
    private Integer tariffCode;
    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;
    private Address address;
}

// Event для создания платежа
@Data
@Builder
public class PaymentCreateRequestEvent implements Serializable {
    private UUID orderId;
    private BigDecimal amount;
    private UUID userId;
}
```

#### 2. Listener для событий от Payment/Delivery сервисов

```java
package com.marketplace.orderservice.messaging;

import com.marketplace.orderservice.event.*;
import com.marketplace.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {
    
    private final OrderService orderService;
    
    @RabbitListener(queues = "${rabbitmq.queue.payment-status-updated}")
    public void handlePaymentStatusUpdated(PaymentStatusUpdatedEvent event) {
        log.info("Payment status updated for order: {}", event.getOrderId());
        orderService.handlePaymentStatusUpdate(event);
    }
    
    @RabbitListener(queues = "${rabbitmq.queue.delivery-status-updated}")
    public void handleDeliveryStatusUpdated(DeliveryStatusUpdatedEvent event) {
        log.info("Delivery status updated for order: {}", event.getOrderId());
        orderService.handleDeliveryStatusUpdate(event);
    }
}
```

### Обновленный процесс создания заказа

```java
@Transactional
public OrderResponseDto createOrder(UUID userId, OrderRequestDto requestDto) {
    // 1. Создание заказа
    Order order = createOrderEntity(userId, requestDto);
    Order savedOrder = orderRepository.save(order);
    
    // 2. Публикация событий
    publishStockDecreaseEvents(requestDto.getItems());
    
    // 3. Создание платежа
    publishPaymentCreateEvent(savedOrder);
    
    // 4. Создание доставки
    publishDeliveryCreateEvent(savedOrder, requestDto);
    
    // 5. Очистка корзины
    publishOrderCompletedEvent(userId, savedOrder.getId());
    
    return orderMapper.toOrderResponseDto(savedOrder);
}

private void publishPaymentCreateEvent(Order order) {
    PaymentCreateRequestEvent event = PaymentCreateRequestEvent.builder()
        .orderId(order.getId())
        .amount(order.getTotalAmount())
        .userId(order.getUserId())
        .build();
    
    rabbitTemplate.convertAndSend(
        paymentExchange,
        "payment.create",
        event
    );
}

private void publishDeliveryCreateEvent(Order order, OrderRequestDto requestDto) {
    DeliveryCreateRequestEvent event = DeliveryCreateRequestEvent.builder()
        .orderId(order.getId())
        .tariffCode(requestDto.getDeliveryTariffCode())
        .recipientName(requestDto.getRecipientName())
        .recipientPhone(requestDto.getRecipientPhone())
        .address(requestDto.getDeliveryAddress())
        .build();
    
    rabbitTemplate.convertAndSend(
        deliveryExchange,
        "delivery.create",
        event
    );
}
```

---

## Docker Compose Обновления

Добавьте новые сервисы в [`docker-compose.yml`](docker-compose.yml):

```yaml
services:
  # ... existing services ...
  
  deliveryservice:
    build: ./deliveryservice
    container_name: deliveryservice
    ports:
      - "8090:8090"
    environment:
      DB_HOST: db
      DB_PORT: 5432
      DB_NAME: deliverydb
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USERNAME: admin
      RABBITMQ_PASSWORD: admin
      CDEK_ACCOUNT: ${CDEK_ACCOUNT}
      CDEK_SECURE_PASSWORD: ${CDEK_SECURE_PASSWORD}
      SERVER_PORT: 8090
    depends_on:
      db:
        condition: service_healthy
      rabbitmq:
        condition: service_started
    networks:
      - microservices
  
  paymentservice:
    build: ./paymentservice
    container_name: paymentservice
    ports:
      - "8091:8091"
    environment:
      DB_HOST: db
      DB_PORT: 5432
      DB_NAME: paymentdb
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USERNAME: admin
      RABBITMQ_PASSWORD: admin
      YOOKASSA_SHOP_ID: ${YOOKASSA_SHOP_ID}
      YOOKASSA_SECRET_KEY: ${YOOKASSA_SECRET_KEY}
      YOOKASSA_RETURN_URL: ${YOOKASSA_RETURN_URL}
      SERVER_PORT: 8091
    depends_on:
      db:
        condition: service_healthy
      rabbitmq:
        condition: service_started
    networks:
      - microservices

# Update postgres databases list
  db:
    environment:
      POSTGRES_MULTIPLE_DATABASES: basket_db,shop_db,keycloak_postgres,userservice_db,productdb,cartdb,newsdb,orderdb,deliverydb,paymentdb
```

---

## Environment Variables

Создайте `.env` файл:

```bash
# CDEK API Credentials
CDEK_ACCOUNT=your_cdek_account
CDEK_SECURE_PASSWORD=your_cdek_password

# YooKassa API Credentials
YOOKASSA_SHOP_ID=your_shop_id
YOOKASSA_SECRET_KEY=your_secret_key
YOOKASSA_RETURN_URL=http://localhost:3000/payment/return
```

---

## Полная Диаграмма Потока

```
┌──────────┐
│  Client  │
└────┬─────┘
     │
     ▼
┌────────────────┐
│  OrderService  │
└────┬───┬───┬───┘
     │   │   │
     │   │   └─────────────────────┐
     │   │                         │
     │   └───────────────┐         │
     │                   │         │
     ▼                   ▼         ▼
┌─────────────┐   ┌──────────┐ ┌─────────────┐
│RabbitMQ     │   │RabbitMQ  │ │RabbitMQ     │
│stock.       │   │payment.  │ │delivery.    │
│decrease     │   │create    │ │create       │
└─────┬───────┘   └────┬─────┘ └─────┬───────┘
      │                │             │
      ▼                ▼             ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ProductService│ │PaymentService│ │DeliveryServ. │
└──────────────┘ └──────┬───────┘ └──────┬───────┘
                        │                │
                        ▼                ▼
                  ┌───────────┐    ┌──────────┐
                  │ YooKassa  │    │   CDEK   │
                  │    API    │    │   API    │
                  └───────────┘    └──────────┘
```

---

## Мониторинг и Отладка

### RabbitMQ Management UI

Доступен по адресу: http://localhost:15672

Логин: `admin`  
Пароль: `admin`

### Полезные команды

```bash
# Просмотр всех exchanges
curl -u admin:admin http://localhost:15672/api/exchanges

# Просмотр всех очередей
curl -u admin:admin http://localhost:15672/api/queues

# Просмотр сообщений в очереди
curl -u admin:admin http://localhost:15672/api/queues/%2F/stock.decrease/get \
  -d '{"count":10,"ackmode":"ack_requeue_false","encoding":"auto"}'

# Логи RabbitMQ
docker logs rabbitmq -f

# Логи конкретного сервиса
docker logs orderservice -f
```

---

## Best Practices

### 1. Idempotency (Идемпотентность)

Всегда используйте идемпотентные операции для обработки событий:

```java
@Transactional
public void handleStockDecrease(StockDecreaseDto dto) {
    // Проверяем, не обработано ли уже это событие
    if (eventProcessingRepository.existsByEventId(dto.getEventId())) {
        log.info("Event already processed: {}", dto.getEventId());
        return;
    }
    
    // Обрабатываем событие
    productSizeService.decreaseStock(dto);
    
    // Сохраняем факт обработки
    eventProcessingRepository.save(new EventProcessing(dto.getEventId()));
}
```

### 2. Dead Letter Queue (DLQ)

Настройте DLQ для неудачных сообщений:

```java
@Bean
public Queue stockDecreaseQueue() {
    return QueueBuilder.durable(stockDecreaseQueue)
        .withArgument("x-dead-letter-exchange", "dlx-exchange")
        .withArgument("x-dead-letter-routing-key", "stock.decrease.dlq")
        .build();
}

@Bean
public Queue deadLetterQueue() {
    return QueueBuilder.durable("stock.decrease.dlq").build();
}
```

### 3. Retry Logic

Настройте retry в application.yml:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          initial-interval: 3000
          max-attempts: 3
          multiplier: 2
          max-interval: 10000
```

### 4. Circuit Breaker

Используйте Resilience4j для внешних API:

```java
@CircuitBreaker(name = "cdek", fallbackMethod = "cdekFallback")
public CdekOrderResponse createOrder(CdekOrderRequest request) {
    return cdekApiClient.createOrder(request);
}

private CdekOrderResponse cdekFallback(CdekOrderRequest request, Exception e) {
    log.error("CDEK API unavailable, using fallback", e);
    // Возвращаем дефолтное значение или выбрасываем ошибку
    throw new CdekUnavailableException("CDEK service unavailable");
}
```

---

## Следующие Шаги

1. **Создайте новые микросервисы**: `deliveryservice` и `paymentservice`
2. **Обновите OrderService**: Добавьте интеграцию с новыми сервисами
3. **Настройте Webhooks**: Зарегистрируйте webhook URL в YooKassa
4. **Тестирование**: Протестируйте полный флоу создания заказа
5. **Мониторинг**: Настройте алерты в Grafana для критичных событий
6. **Документация API**: Обновите Swagger/OpenAPI документацию

---

## Дополнительные Ресурсы

- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [CDEK API Documentation](https://api-docs.cdek.ru/)
- [YooKassa API Documentation](https://yookassa.ru/developers/api)
- [Spring AMQP Documentation](https://spring.io/projects/spring-amqp)
