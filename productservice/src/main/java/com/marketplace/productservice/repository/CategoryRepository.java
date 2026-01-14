package com.marketplace.productservice.repository;

import com.marketplace.productservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    
    List<Category> findByParentId(UUID parentId);
    
    List<Category> findByParentIsNull();
    
    List<Category> findByShopId(UUID shopId);
    
    void deleteByShopId(UUID shopId);
}
