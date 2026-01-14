package com.marketplace.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandRequestDto {

    private UUID shopId;

    @NotBlank(message = "Brand name is required")
    private String name;

    private String logoUrl;
}
