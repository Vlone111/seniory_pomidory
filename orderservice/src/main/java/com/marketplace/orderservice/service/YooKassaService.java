package com.marketplace.orderservice.service;

import com.marketplace.orderservice.config.YooKassaProperties;
import com.marketplace.orderservice.dto.yookassa.YooKassaPaymentRequest;
import com.marketplace.orderservice.dto.yookassa.YooKassaPaymentResponse;
import com.marketplace.orderservice.entity.YooKassaPayment;
import com.marketplace.orderservice.repository.YooKassaPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class YooKassaService {
    
    private final YooKassaProperties yooKassaProperties;
    private final WebClient webClient;
    private final YooKassaPaymentRepository paymentRepository;
    
    public Mono<YooKassaPaymentResponse> createPayment(String amount, String currency, String description) {
        return createPayment(amount, currency, description, null, null);
    }

    public Mono<YooKassaPaymentResponse> createPayment(String amount, String currency, String description, 
                                                        String orderId, String returnUrl) {
        log.info("Creating YooKassa payment for amount: {} {}, description: {}, orderId: {}", 
                amount, currency, description, orderId);
        
        String idempotenceKey = UUID.randomUUID().toString();
        String authHeader = createAuthHeader();
        
        // Используем redirect для редиректа на страницу оплаты YooKassa
        String effectiveReturnUrl = (returnUrl != null && !returnUrl.isEmpty()) 
                ? returnUrl 
                : "http://localhost:3001/checkout?payment_status=success";
        
        YooKassaPaymentRequest.Confirmation confirmation = YooKassaPaymentRequest.Confirmation.builder()
                .type("redirect")
                .returnUrl(effectiveReturnUrl)
                .build();
        
        // Добавляем orderId в metadata если есть
        java.util.Map<String, String> metadata = null;
        if (orderId != null && !orderId.isEmpty()) {
            metadata = java.util.Map.of("order_id", orderId);
        }
        
        YooKassaPaymentRequest request = YooKassaPaymentRequest.builder()
                .amount(YooKassaPaymentRequest.Amount.builder()
                        .value(amount)
                        .currency(currency)
                        .build())
                .confirmation(confirmation)
                .capture(true)
                .description(description)
                .metadata(metadata)
                .build();
        
        log.info("Request payload: {}", request);
        log.info("Auth header: {}", authHeader.substring(0, 10) + "...");
        
        return webClient.post()
                .uri(yooKassaProperties.getApiUrl())
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header("Idempotence-Key", idempotenceKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    response -> response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException("YooKassa API error: " + response.statusCode() + " - " + body))))
                .bodyToMono(YooKassaPaymentResponse.class)
                .doOnSuccess(response -> {
                    log.info("Successfully created YooKassa payment with ID: {}", response.getId());
                    savePaymentToDatabase(response);
                })
                .doOnError(error -> {
                    log.error("Error creating YooKassa payment: {}", error.getMessage());
                    log.error("Full error: {}", error);
                });
    }
    
    public Mono<YooKassaPaymentResponse> getPaymentStatus(String paymentId) {
        log.info("Getting YooKassa payment status for payment ID: {}", paymentId);
        
        String authHeader = createAuthHeader();
        
        return webClient.get()
                .uri(yooKassaProperties.getApiUrl() + "/" + paymentId)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(YooKassaPaymentResponse.class)
                .doOnSuccess(response -> log.info("Successfully retrieved payment status for ID: {}, status: {}", paymentId, response.getStatus()))
                .doOnError(error -> {
                    log.error("Error retrieving payment status for ID {}: {}", paymentId, error.getMessage());
                    log.error("Full error: {}", error);
                });
    }
    
    private String createAuthHeader() {
        String credentials = yooKassaProperties.getShopId() + ":" + yooKassaProperties.getSecretKey();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }
    
    private void savePaymentToDatabase(YooKassaPaymentResponse response) {
        try {
            YooKassaPayment.YooKassaPaymentBuilder builder = YooKassaPayment.builder()
                    .paymentId(response.getId())
                    .amount(new BigDecimal(response.getAmount().getValue()))
                    .currency(response.getAmount().getCurrency())
                    .description(response.getDescription())
                    .status(response.getStatus())
                    .paid(response.isPaid())
                    .test(response.isTest())
                    .refundable(response.isRefundable())
                    .createdAt(LocalDateTime.parse(response.getCreatedAt().replace("Z", "")));
            
            // Для redirect типа сохраняем URL, для embedded - токен
            if (response.getConfirmation() != null) {
                if (response.getConfirmation().getConfirmationToken() != null) {
                    builder.confirmationToken(response.getConfirmation().getConfirmationToken());
                } else if (response.getConfirmation().getConfirmationUrl() != null) {
                    builder.confirmationToken(response.getConfirmation().getConfirmationUrl());
                }
            }
            
            // Добавляем metadata и orderId если есть
            if (response.getMetadata() instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> metadata = (java.util.Map<String, Object>) response.getMetadata();
                builder.metadata(metadata);
                
                // Извлекаем orderId из metadata
                Object orderIdObj = metadata.get("order_id");
                if (orderIdObj != null) {
                    try {
                        builder.orderId(UUID.fromString(orderIdObj.toString()));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid orderId format in metadata: {}", orderIdObj);
                    }
                }
            }
            
            YooKassaPayment payment = builder.build();
            paymentRepository.save(payment);
            log.info("Payment saved to database with ID: {}, orderId: {}", payment.getId(), payment.getOrderId());
        } catch (Exception e) {
            log.error("Error saving payment to database: {}", e.getMessage(), e);
        }
    }
}
