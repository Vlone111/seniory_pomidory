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

    // Yandex Delivery fields
    @Column(name = "yandex_pickup_point_id", length = 100)
    private String yandexPickupPointId;

    @Column(name = "yandex_pickup_point_address", length = 500)
    private String yandexPickupPointAddress;

    @Column(name = "yandex_pickup_point_name", length = 255)
    private String yandexPickupPointName;

    @Column(name = "yandex_latitude")
    private Double yandexLatitude;

    @Column(name = "yandex_longitude")
    private Double yandexLongitude;

    @Column(name = "yandex_delivery_price", precision = 10, scale = 2)
    private BigDecimal yandexDeliveryPrice;

    @Column(name = "yandex_delivery_term")
    private Integer yandexDeliveryTerm;

    @Column(name = "yandex_pickup_point_type", length = 50)
    private String yandexPickupPointType;

    @Column(name = "yandex_work_schedule", length = 500)
    private String yandexWorkSchedule;

    @Column(name = "yandex_phone", length = 50)
    private String yandexPhone;

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
}
