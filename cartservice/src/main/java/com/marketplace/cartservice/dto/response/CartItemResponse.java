package com.marketplace.cartservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private UUID productId;
    private UUID shopId;
    private String name;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
    private String imageUrl;
}
