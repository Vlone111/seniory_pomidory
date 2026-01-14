package com.marketplace.productservice.repository;

import com.marketplace.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    
    List<Product> findAllByShopId(UUID shopId);
    
    List<Product> findAllByShopIdAndIsActive(UUID shopId, Boolean isActive);
    
    void deleteByShopId(UUID shopId);
    
    @Query("SELECT p FROM Product p WHERE " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:brandId IS NULL OR p.brand.id = :brandId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:searchText IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchText, '%')))")
    List<Product> searchProducts(
        @Param("categoryId") UUID categoryId,
        @Param("brandId") UUID brandId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("searchText") String searchText
    );
}
