package com.marketplace.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDto {

    @NotNull(message = "Recipient ID is required")
    private UUID recipientId;

    @NotNull(message = "Delivery method ID is required")
    private UUID deliveryMethodId;

    @NotNull(message = "Payment method ID is required")
    private UUID paymentMethodId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequestDto> items;
}
