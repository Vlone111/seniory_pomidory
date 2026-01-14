package com.example.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shops", indexes = {
    @Index(name = "idx_owner_id", columnList = "owner_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shop {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "shop_name", nullable = false, unique = true)
    private String shopName;

    @Column(name = "shop_url", nullable = false, unique = true)
    private String shopUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "pfp_url")
    private String pfpUrl;

    @Column(name = "design_code", nullable = false)
    private String designCode;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    

}
