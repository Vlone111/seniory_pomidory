package com.marketplace.orderservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponseDto {

    private UUID id;
    private UUID productId;
    private UUID shopId;
    private Integer quantity;
    private BigDecimal pricePerItem;
    private BigDecimal subtotal;
}
