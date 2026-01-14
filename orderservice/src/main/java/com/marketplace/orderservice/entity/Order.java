package com.marketplace.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user_id", columnList = "user_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_method_id", nullable = false)
    private DeliveryMethod deliveryMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    // CDEK delivery fields
    @Column(name = "cdek_pvz_code", length = 50)
    private String cdekPvzCode;

    @Column(name = "cdek_pvz_address", length = 500)
    private String cdekPvzAddress;

    @Column(name = "cdek_city_code", length = 20)
    private String cdekCityCode;

    @Column(name = "cdek_tariff_code")
    private Integer cdekTariffCode;

    @Column(name = "cdek_delivery_sum", precision = 10, scale = 2)
    private BigDecimal cdekDeliverySum;

    @Column(name = "cdek_period_min")
    private Integer cdekPeriodMin;

    @Column(name = "cdek_period_max")
    private Integer cdekPeriodMax;

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
}
