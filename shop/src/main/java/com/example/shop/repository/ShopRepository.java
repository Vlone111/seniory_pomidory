package com.example.shop.repository;

import com.example.shop.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {
    
    Optional<Shop> findByShopUrl(String shopUrl);
    
    Optional<Shop> findByShopName(String shopName);
    
    List<Shop> findByOwnerId(UUID ownerId);
    
    boolean existsByShopUrl(String shopUrl);
    
    boolean existsByShopName(String shopName);
}
