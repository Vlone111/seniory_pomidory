package com.marketplace.orderservice.repository;

import com.marketplace.orderservice.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    
    Optional<PaymentMethod> findByName(String name);
    
    boolean existsByName(String name);
}
