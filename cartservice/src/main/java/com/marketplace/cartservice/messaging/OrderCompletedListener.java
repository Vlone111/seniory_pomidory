package com.marketplace.cartservice.messaging;

import com.marketplace.cartservice.dto.event.OrderCompletedEvent;
import com.marketplace.cartservice.service.CartServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCompletedListener {

    private final CartServiceImpl cartService;

    @RabbitListener(queues = "${rabbitmq.queue.order-completed}")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Received order completed event for user: {}", event.getUserId());
        
        try {
            cartService.clearCart(event.getUserId());
            log.info("Cart cleared successfully after order completion for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Error clearing cart after order completion for user: {}", 
                event.getUserId(), e);
            throw e;
        }
    }
}
