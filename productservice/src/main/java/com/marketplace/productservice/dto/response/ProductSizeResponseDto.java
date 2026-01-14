package com.marketplace.productservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSizeResponseDto {

    private UUID id;

    private UUID productId;

    private SizeResponseDto size;

    private Integer quantityAvailable;
}
