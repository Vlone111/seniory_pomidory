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
public class CdekDeliveryDto {

    private String pvzCode;

    private String pvzAddress;

    private String cityCode;

    private Integer tariffCode;

    private BigDecimal deliverySum;

    private Integer periodMin;

    private Integer periodMax;
}
