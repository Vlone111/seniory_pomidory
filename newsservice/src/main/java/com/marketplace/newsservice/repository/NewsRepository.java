package com.marketplace.newsservice.repository;

import com.marketplace.newsservice.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NewsRepository extends JpaRepository<News, UUID> {
    
    Optional<News> findBySlugAndDeletedFalse(String slug);
    
    Optional<News> findBySlugAndIsPublishedTrueAndDeletedFalse(String slug);
    
    Page<News> findByIsPublishedTrueAndDeletedFalseOrderByPublishedAtDesc(Pageable pageable);
    
    Page<News> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT n FROM News n WHERE n.deleted = false AND " +
           "(:isPublished IS NULL OR n.isPublished = :isPublished) " +
           "ORDER BY n.createdAt DESC")
    Page<News> findAllByPublishedStatus(@Param("isPublished") Boolean isPublished, Pageable pageable);
    
    boolean existsBySlugAndDeletedFalse(String slug);
    
    boolean existsBySlugAndIdNotAndDeletedFalse(String slug, UUID id);
}
