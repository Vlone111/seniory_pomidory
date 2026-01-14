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
public class BrandResponseDto {

    private UUID id;

    private UUID shopId;

    private String name;

    private String logoUrl;
}
