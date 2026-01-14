package com.marketplace.orderservice.service;

import com.marketplace.orderservice.dto.yookassa.YooKassaWebhookEvent;
import com.marketplace.orderservice.entity.Order;
import com.marketplace.orderservice.entity.OrderStatus;
import com.marketplace.orderservice.entity.YooKassaPayment;
import com.marketplace.orderservice.event.StockDecreaseEvent;
import com.marketplace.orderservice.repository.OrderRepository;
import com.marketplace.orderservice.repository.YooKassaPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для обработки webhook событий от платежных систем
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookService {

    private final YooKassaPaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.order}")
    private String orderExchange;

    @Value("${rabbitmq.routing-key.stock-decrease}")
    private String stockDecreaseRoutingKey;

    /**
     * Обработка webhook события от YooKassa
     */
    @Transactional
    public void processWebhookEvent(YooKassaWebhookEvent event) {
        String eventType = event.getEvent();
        YooKassaWebhookEvent.PaymentObject paymentObject = event.getObject();

        if (paymentObject == null) {
            log.warn("Received webhook event without payment object");
            return;
        }

        String paymentId = paymentObject.getId();
        String status = paymentObject.getStatus();

        log.info("Processing webhook event: type={}, paymentId={}, status={}", eventType, paymentId, status);

        switch (eventType) {
            case "payment.succeeded":
                handlePaymentSucceeded(paymentObject);
                break;
            case "payment.canceled":
                handlePaymentCanceled(paymentObject);
                break;
            case "payment.waiting_for_capture":
                handlePaymentWaitingForCapture(paymentObject);
                break;
            case "refund.succeeded":
                handleRefundSucceeded(paymentObject);
                break;
            default:
                log.info("Ignoring unhandled event type: {}", eventType);
        }
    }

    /**
     * Обработка успешного платежа
     */
    private void handlePaymentSucceeded(YooKassaWebhookEvent.PaymentObject paymentObject) {
        String paymentId = paymentObject.getId();
        log.info("Processing successful payment: {}", paymentId);

        // Обновляем статус платежа в БД
        Optional<YooKassaPayment> paymentOpt = paymentRepository.findByPaymentId(paymentId);
        if (paymentOpt.isPresent()) {
            YooKassaPayment payment = paymentOpt.get();
            payment.setStatus("succeeded");
            payment.setPaid(true);
            if (paymentObject.getCapturedAt() != null) {
                payment.setCapturedAt(LocalDateTime.parse(paymentObject.getCapturedAt().replace("Z", "")));
            }
            paymentRepository.save(payment);
            log.info("Payment status updated to succeeded: {}", paymentId);

            // Получаем orderId из metadata или из payment
            UUID orderId = payment.getOrderId();
            if (orderId == null && paymentObject.getMetadata() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) paymentObject.getMetadata();
                String orderIdStr = (String) metadata.get("order_id");
                if (orderIdStr != null) {
                    orderId = UUID.fromString(orderIdStr);
                }
            }

            if (orderId != null) {
                processOrderPaymentSuccess(orderId);
            } else {
                log.warn("No orderId found for payment: {}", paymentId);
            }
        } else {
            log.warn("Payment not found in database: {}", paymentId);
        }
    }

    /**
     * Обработка успешной оплаты заказа - уменьшение остатков товара
     */
    private void processOrderPaymentSuccess(UUID orderId) {
        log.info("Processing order payment success: {}", orderId);

        Optional<Order> orderOpt = orderRepository.findByIdWithDetails(orderId);
        if (orderOpt.isEmpty()) {
            log.error("Order not found: {}", orderId);
            return;
        }

        Order order = orderOpt.get();
        
        // Обновляем статус заказа на PAID
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        log.info("Order status updated to PAID: {}", orderId);

        // Отправляем события на уменьшение остатков товара
        order.getItems().forEach(item -> {
            StockDecreaseEvent event = StockDecreaseEvent.builder()
                    .productId(item.getProductId())
                    .sizeId(null) // TODO: добавить sizeId если есть
                    .quantity(item.getQuantity())
                    .build();

            try {
                rabbitTemplate.convertAndSend(orderExchange, stockDecreaseRoutingKey, event);
                log.info("Stock decrease event sent for product: {}, quantity: {}", 
                        item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Failed to send stock decrease event for product: {}", item.getProductId(), e);
            }
        });

        log.info("Order payment processing completed: {}", orderId);
    }

    /**
     * Обработка отмененного платежа
     */
    private void handlePaymentCanceled(YooKassaWebhookEvent.PaymentObject paymentObject) {
        String paymentId = paymentObject.getId();
        log.info("Processing canceled payment: {}", paymentId);

        Optional<YooKassaPayment> paymentOpt = paymentRepository.findByPaymentId(paymentId);
        if (paymentOpt.isPresent()) {
            YooKassaPayment payment = paymentOpt.get();
            payment.setStatus("canceled");
            payment.setPaid(false);
            paymentRepository.save(payment);
            log.info("Payment status updated to canceled: {}", paymentId);

            // Обновляем статус заказа если есть
            if (payment.getOrderId() != null) {
                Optional<Order> orderOpt = orderRepository.findById(payment.getOrderId());
                orderOpt.ifPresent(order -> {
                    order.setStatus(OrderStatus.CANCELED);
                    orderRepository.save(order);
                    log.info("Order status updated to CANCELLED: {}", payment.getOrderId());
                });
            }
        }
    }

    /**
     * Обработка платежа, ожидающего подтверждения
     */
    private void handlePaymentWaitingForCapture(YooKassaWebhookEvent.PaymentObject paymentObject) {
        String paymentId = paymentObject.getId();
        log.info("Payment waiting for capture: {}", paymentId);

        Optional<YooKassaPayment> paymentOpt = paymentRepository.findByPaymentId(paymentId);
        if (paymentOpt.isPresent()) {
            YooKassaPayment payment = paymentOpt.get();
            payment.setStatus("waiting_for_capture");
            paymentRepository.save(payment);
        }
    }

    /**
     * Обработка успешного возврата
     */
    private void handleRefundSucceeded(YooKassaWebhookEvent.PaymentObject paymentObject) {
        String paymentId = paymentObject.getId();
        log.info("Processing refund for payment: {}", paymentId);
        // TODO: реализовать возврат товара на склад если нужно
    }
}
