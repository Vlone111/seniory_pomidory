package com.marketplace.orderservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class OrderItemRequestDto {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Shop ID is required")
    private UUID shopId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Price per item is required")
    @Min(value = 0, message = "Price must be positive")
    private BigDecimal pricePerItem;
}
