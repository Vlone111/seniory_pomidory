package com.example.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecipientDTO {

    @NotBlank(message = "Address is required")
    private String address;

    private String zipCode;
    private String city;
    private String comment;
}
