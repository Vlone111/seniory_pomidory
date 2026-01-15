package com.marketplace.orderservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YandexDeliveryDto {

    // Идентификатор выбранного ПВЗ
    private String pickupPointId;

    // Адрес ПВЗ
    private String pickupPointAddress;

    // Название ПВЗ
    private String pickupPointName;

    // Координаты ПВЗ
    private Double latitude;
    private Double longitude;

    // Стоимость доставки
    private BigDecimal deliveryPrice;

    // Срок доставки (в днях)
    private Integer deliveryTerm;

    // Тип ПВЗ (pickup_point или terminal)
    private String pickupPointType;

    // Дополнительная информация
    private String workSchedule;
    private String phone;
}
