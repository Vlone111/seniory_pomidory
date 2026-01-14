package com.marketplace.productservice.repository;

import com.marketplace.productservice.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {
    
    Optional<Brand> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Brand> findByShopId(UUID shopId);
    
    void deleteByShopId(UUID shopId);
}
