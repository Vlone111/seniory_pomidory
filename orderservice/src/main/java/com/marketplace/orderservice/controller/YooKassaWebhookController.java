package com.marketplace.orderservice.controller;

import com.marketplace.orderservice.dto.yookassa.YooKassaWebhookEvent;
import com.marketplace.orderservice.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для обработки webhook уведомлений от YooKassa
 * Endpoint должен быть доступен извне (через ngrok для тестирования)
 */
@RestController
@RequestMapping("/api/webhooks/yookassa")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class YooKassaWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    /**
     * Обработка уведомлений о платежах от YooKassa
     * YooKassa отправляет POST запрос при изменении статуса платежа
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody YooKassaWebhookEvent event) {
        log.info("Received YooKassa webhook: type={}, paymentId={}", 
                event.getEvent(), 
                event.getObject() != null ? event.getObject().getId() : "null");
        
        try {
            paymentWebhookService.processWebhookEvent(event);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing YooKassa webhook: {}", e.getMessage(), e);
            // Возвращаем 200, чтобы YooKassa не повторяла запрос
            // Ошибки логируем и обрабатываем асинхронно
            return ResponseEntity.ok("OK");
        }
    }

    /**
     * Health check endpoint для проверки доступности webhook
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("YooKassa webhook is healthy");
    }
}
