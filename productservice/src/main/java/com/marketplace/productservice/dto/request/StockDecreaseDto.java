package com.marketplace.productservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDecreaseDto {

    private UUID productId;

    private UUID sizeId;

    private Integer quantity;
}
