package com.marketplace.orderservice.dto.yookassa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для webhook событий от YooKassa
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YooKassaWebhookEvent {

    /**
     * Тип события: payment.succeeded, payment.canceled, payment.waiting_for_capture, refund.succeeded
     */
    private String event;

    /**
     * Тип объекта (всегда "notification")
     */
    private String type;

    /**
     * Объект платежа с информацией
     */
    private PaymentObject object;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentObject {
        
        private String id;
        
        private String status;
        
        private Amount amount;
        
        @JsonProperty("income_amount")
        private Amount incomeAmount;
        
        private String description;
        
        private Recipient recipient;
        
        @JsonProperty("payment_method")
        private PaymentMethod paymentMethod;
        
        @JsonProperty("captured_at")
        private String capturedAt;
        
        @JsonProperty("created_at")
        private String createdAt;
        
        private boolean test;
        
        private boolean paid;
        
        private boolean refundable;
        
        private Object metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private String value;
        private String currency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Recipient {
        @JsonProperty("account_id")
        private String accountId;
        
        @JsonProperty("gateway_id")
        private String gatewayId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentMethod {
        private String type;
        private String id;
        private boolean saved;
        private String title;
    }
}
