package com.marketplace.orderservice.repository;

import com.marketplace.orderservice.entity.Order;
import com.marketplace.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId")
    Page<Order> findByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status = :status")
    List<Order> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") OrderStatus status);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") UUID orderId);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.deliveryMethod LEFT JOIN FETCH o.paymentMethod WHERE o.id = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") UUID orderId);
}
