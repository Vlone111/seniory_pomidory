package com.marketplace.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_item_order_id", columnList = "order_id"),
    @Index(name = "idx_order_item_product_id", columnList = "product_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_per_item", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerItem;

    public BigDecimal getSubtotal() {
        return pricePerItem.multiply(BigDecimal.valueOf(quantity));
    }
}
