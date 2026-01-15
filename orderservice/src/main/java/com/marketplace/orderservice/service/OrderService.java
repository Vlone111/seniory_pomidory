package com.marketplace.orderservice.service;

import com.marketplace.orderservice.dto.mapper.OrderMapper;
import com.marketplace.orderservice.dto.request.OrderItemRequestDto;
import com.marketplace.orderservice.dto.request.OrderRequestDto;
import com.marketplace.orderservice.dto.request.UpdateOrderStatusDto;
import com.marketplace.orderservice.dto.response.OrderResponseDto;
import com.marketplace.orderservice.entity.*;
import com.marketplace.orderservice.event.OrderCompletedEvent;
import com.marketplace.orderservice.event.StockDecreaseEvent;
import com.marketplace.orderservice.exception.*;
import com.marketplace.orderservice.repository.DeliveryMethodRepository;
import com.marketplace.orderservice.repository.OrderRepository;
import com.marketplace.orderservice.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final DeliveryMethodRepository deliveryMethodRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderMapper orderMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.order}")
    private String orderExchange;

    @Value("${rabbitmq.routing-key.stock-decrease}")
    private String stockDecreaseRoutingKey;

    @Value("${rabbitmq.routing-key.order-completed}")
    private String orderCompletedRoutingKey;

    @Transactional
    public OrderResponseDto createOrder(UUID userId, OrderRequestDto requestDto) {
        log.info("Creating order for user: {}", userId);

        // Fetch delivery and payment methods
        DeliveryMethod deliveryMethod = deliveryMethodRepository.findById(requestDto.getDeliveryMethodId())
                .orElseThrow(() -> new DeliveryMethodNotFoundException(
                        "Delivery method not found with id: " + requestDto.getDeliveryMethodId()));

        PaymentMethod paymentMethod = paymentMethodRepository.findById(requestDto.getPaymentMethodId())
                .orElseThrow(() -> new PaymentMethodNotFoundException(
                        "Payment method not found with id: " + requestDto.getPaymentMethodId()));

        // Calculate total amount
        BigDecimal itemsTotal = requestDto.getItems().stream()
                .map(item -> item.getPricePerItem().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Use Yandex delivery price if present, otherwise use delivery method price
        BigDecimal deliveryPrice = (requestDto.getYandexDelivery() != null && requestDto.getYandexDelivery().getDeliveryPrice() != null)
                ? requestDto.getYandexDelivery().getDeliveryPrice()
                : deliveryMethod.getPrice();

        BigDecimal totalAmount = itemsTotal
                .add(deliveryPrice)
                .add(paymentMethod.getPrice());

        // Create order
        Order.OrderBuilder orderBuilder = Order.builder()
                .userId(userId)
                .recipientId(requestDto.getRecipientId())
                .deliveryMethod(deliveryMethod)
                .paymentMethod(paymentMethod)
                .status(OrderStatus.NEW)
                .totalAmount(totalAmount);

        // Add Yandex delivery data if present
        if (requestDto.getYandexDelivery() != null) {
            var yandex = requestDto.getYandexDelivery();
            orderBuilder
                    .yandexPickupPointId(yandex.getPickupPointId())
                    .yandexPickupPointAddress(yandex.getPickupPointAddress())
                    .yandexPickupPointName(yandex.getPickupPointName())
                    .yandexLatitude(yandex.getLatitude())
                    .yandexLongitude(yandex.getLongitude())
                    .yandexDeliveryPrice(yandex.getDeliveryPrice())
                    .yandexDeliveryTerm(yandex.getDeliveryTerm())
                    .yandexPickupPointType(yandex.getPickupPointType())
                    .yandexWorkSchedule(yandex.getWorkSchedule())
                    .yandexPhone(yandex.getPhone());
        }

        Order order = orderBuilder.build();

        // Add order items
        for (OrderItemRequestDto itemDto : requestDto.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .productId(itemDto.getProductId())
                    .shopId(itemDto.getShopId())
                    .quantity(itemDto.getQuantity())
                    .pricePerItem(itemDto.getPricePerItem())
                    .build();
            order.addItem(orderItem);
        }

        // Save order
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with id: {}", savedOrder.getId());

        // НЕ отправляем события на уменьшение остатков при создании заказа
        // Остатки уменьшаются только после подтверждения оплаты через webhook
        // publishStockDecreaseEvents(requestDto.getItems());
        
        // Отправляем событие о создании заказа (для уведомлений и т.д.)
        publishOrderCompletedEvent(userId, savedOrder.getId());

        return orderMapper.toOrderResponseDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getUserOrders(UUID userId) {
        log.info("Fetching orders for user: {}", userId);
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orderMapper.toOrderResponseDtoList(orders);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(UUID orderId, UUID userId) {
        log.info("Fetching order: {} for user: {}", orderId, userId);
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        // Verify user owns this order
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to view this order");
        }

        return orderMapper.toOrderResponseDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersByShopId(UUID shopId) {
        log.info("Fetching orders for shop: {}", shopId);
        List<Order> orders = orderRepository.findByShopIdWithDetails(shopId);
        return orderMapper.toOrderResponseDtoList(orders);
    }

    @Transactional
    public OrderResponseDto updateOrderStatus(UUID orderId, UpdateOrderStatusDto statusDto) {
        log.info("Updating order status: {} to {}", orderId, statusDto.getStatus());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        order.setStatus(statusDto.getStatus());
        Order updatedOrder = orderRepository.save(order);
        log.info("Order status updated successfully");

        return orderMapper.toOrderResponseDto(updatedOrder);
    }

    private void publishStockDecreaseEvents(List<OrderItemRequestDto> items) {
        log.info("Publishing stock decrease events for {} items", items.size());
        for (OrderItemRequestDto item : items) {
            StockDecreaseEvent event = StockDecreaseEvent.builder()
                    .productId(item.getProductId())
                    .sizeId(null) // If you have size information, add it to OrderItemRequestDto
                    .quantity(item.getQuantity())
                    .build();

            try {
                rabbitTemplate.convertAndSend(orderExchange, stockDecreaseRoutingKey, event);
                log.debug("Stock decrease event sent for product: {}", item.getProductId());
            } catch (Exception e) {
                log.error("Failed to send stock decrease event for product: {}", item.getProductId(), e);
                // Consider implementing retry logic or dead letter queue
            }
        }
    }

    private void publishOrderCompletedEvent(UUID userId, UUID orderId) {
        log.info("Publishing order completed event for user: {}", userId);
        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .userId(userId)
                .orderId(orderId)
                .build();

        try {
            rabbitTemplate.convertAndSend(orderExchange, orderCompletedRoutingKey, event);
            log.debug("Order completed event sent");
        } catch (Exception e) {
            log.error("Failed to send order completed event", e);
            // Consider implementing retry logic or dead letter queue
        }
    }
}
