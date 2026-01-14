package com.example.shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {

    @NotBlank(message = "Shop name is required")
    private String shopName;

    @NotBlank(message = "Shop URL is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Shop URL must contain only lowercase letters, numbers, and hyphens")
    private String shopUrl;

    private String description;

    private String pfpUrl;

    @NotBlank(message = "Design code is required")
    private String designCode;

}
