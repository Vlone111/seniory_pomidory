package com.marketplace.orderservice.dto.response;

import com.marketplace.orderservice.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDto {

    private UUID id;
    private UUID userId;
    private UUID recipientId;
    private DeliveryMethodDto deliveryMethod;
    private PaymentMethodDto paymentMethod;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private List<OrderItemResponseDto> items;

    // Yandex Delivery fields
    private String yandexPickupPointId;
    private String yandexPickupPointAddress;
    private String yandexPickupPointName;
    private Double yandexLatitude;
    private Double yandexLongitude;
    private BigDecimal yandexDeliveryPrice;
    private Integer yandexDeliveryTerm;
    private String yandexPickupPointType;
    private String yandexWorkSchedule;
    private String yandexPhone;
}
