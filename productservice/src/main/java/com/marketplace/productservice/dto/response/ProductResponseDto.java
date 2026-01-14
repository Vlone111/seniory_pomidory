package com.marketplace.productservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {

    private UUID id;

    private UUID shopId;

    private String name;

    private String description;

    private BigDecimal price;

    private CategoryResponseDto category;

    private BrandResponseDto brand;

    private List<String> imageUrls;

    private Boolean isActive;

    private UUID createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
