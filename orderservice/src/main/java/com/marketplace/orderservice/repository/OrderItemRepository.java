package com.marketplace.orderservice.repository;

import com.marketplace.orderservice.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") UUID orderId);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.productId = :productId")
    List<OrderItem> findByProductId(@Param("productId") UUID productId);
}
