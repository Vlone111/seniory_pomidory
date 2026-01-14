package com.marketplace.orderservice.repository;

import com.marketplace.orderservice.entity.DeliveryMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryMethodRepository extends JpaRepository<DeliveryMethod, UUID> {
    
    Optional<DeliveryMethod> findByName(String name);
    
    boolean existsByName(String name);
}
