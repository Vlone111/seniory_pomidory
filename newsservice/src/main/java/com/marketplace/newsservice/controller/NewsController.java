package com.marketplace.newsservice.controller;

import com.marketplace.newsservice.dto.request.NewsRequestDto;
import com.marketplace.newsservice.dto.response.NewsDetailDto;
import com.marketplace.newsservice.dto.response.NewsResponseDto;
import com.marketplace.newsservice.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final NewsService newsService;

    /**
     * PUBLIC ENDPOINT: Get paginated list of published news
     */
    @GetMapping
    public ResponseEntity<Page<NewsResponseDto>> getPublishedNews(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NewsResponseDto> news = newsService.getPublishedNews(pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * PUBLIC ENDPOINT: Get news article by slug
     */
    @GetMapping("/{slug}")
    public ResponseEntity<NewsDetailDto> getNewsBySlug(@PathVariable String slug) {
        NewsDetailDto news = newsService.getNewsBySlug(slug);
        return ResponseEntity.ok(news);
    }

    /**
     * ADMIN ENDPOINT: Create news article
     */
    @PostMapping
    public ResponseEntity<NewsDetailDto> createNews(
        @RequestHeader("X-User-Id") String userId,
        @Valid @RequestBody NewsRequestDto requestDto
    ) {
        UUID adminId = UUID.fromString(userId);
        NewsDetailDto news = newsService.createNews(requestDto, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(news);
    }

    /**
     * ADMIN ENDPOINT: Update news article
     */
    @PutMapping("/{id}")
    public ResponseEntity<NewsDetailDto> updateNews(
        @PathVariable UUID id,
        @RequestHeader("X-User-Id") String userId,
        @Valid @RequestBody NewsRequestDto requestDto
    ) {
        NewsDetailDto news = newsService.updateNews(id, requestDto);
        return ResponseEntity.ok(news);
    }

    /**
     * ADMIN ENDPOINT: Soft delete news article
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(
        @PathVariable UUID id,
        @RequestHeader("X-User-Id") String userId
    ) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * ADMIN ENDPOINT: Get all news (including unpublished)
     */
    @GetMapping("/admin/all")
    public ResponseEntity<Page<NewsResponseDto>> getAllNews(
        @RequestHeader("X-User-Id") String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NewsResponseDto> news = newsService.getAllNews(pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * ADMIN ENDPOINT: Get news by published status
     */
    @GetMapping("/admin/filter")
    public ResponseEntity<Page<NewsResponseDto>> getNewsByStatus(
        @RequestHeader("X-User-Id") String userId,
        @RequestParam(required = false) Boolean isPublished,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NewsResponseDto> news = newsService.getNewsByPublishedStatus(isPublished, pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * ADMIN ENDPOINT: Get news by slug (including unpublished)
     */
    @GetMapping("/admin/slug/{slug}")
    public ResponseEntity<NewsDetailDto> getNewsBySlugAdmin(
        @RequestHeader("X-User-Id") String userId,
        @PathVariable String slug
    ) {
        NewsDetailDto news = newsService.getNewsBySlugAdmin(slug);
        return ResponseEntity.ok(news);
    }

    /**
     * ADMIN ENDPOINT: Get news by ID
     */
    @GetMapping("/admin/{id}")
    public ResponseEntity<NewsDetailDto> getNewsById(
        @RequestHeader("X-User-Id") String userId,
        @PathVariable UUID id
    ) {
        NewsDetailDto news = newsService.getNewsById(id);
        return ResponseEntity.ok(news);
    }

    /**
     * ADMIN ENDPOINT: Permanently delete news article
     */
    @DeleteMapping("/admin/{id}/permanent")
    public ResponseEntity<Void> permanentlyDeleteNews(
        @PathVariable UUID id,
        @RequestHeader("X-User-Id") String userId
    ) {
        newsService.permanentlyDeleteNews(id);
        return ResponseEntity.noContent().build();
    }
}
