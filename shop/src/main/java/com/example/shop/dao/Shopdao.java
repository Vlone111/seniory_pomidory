package com.example.shop.dao;

import com.example.shop.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface Shopdao extends JpaRepository<Shop,Long> {
    Optional<Shop> findByIdAndOwnerId(Long id, UUID ownerId);
}
