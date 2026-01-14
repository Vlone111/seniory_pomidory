package com.marketplace.orderservice.dto.request;

import com.marketplace.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusDto {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}
