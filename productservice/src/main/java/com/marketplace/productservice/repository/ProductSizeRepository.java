package com.marketplace.productservice.repository;

import com.marketplace.productservice.entity.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductSizeRepository extends JpaRepository<ProductSize, UUID> {
    
    List<ProductSize> findByProductId(UUID productId);
    
    Optional<ProductSize> findByProductIdAndSizeId(UUID productId, UUID sizeId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps FROM ProductSize ps WHERE ps.product.id = :productId AND ps.size.id = :sizeId")
    Optional<ProductSize> findByProductIdAndSizeIdWithLock(
        @Param("productId") UUID productId, 
        @Param("sizeId") UUID sizeId
    );
}
