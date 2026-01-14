package com.marketplace.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDecreaseEvent implements Serializable {

    private UUID productId;
    private UUID sizeId;
    private Integer quantity;
}
