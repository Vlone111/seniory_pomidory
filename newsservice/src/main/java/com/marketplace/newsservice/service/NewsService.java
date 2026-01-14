package com.marketplace.newsservice.service;

import com.marketplace.newsservice.dto.request.NewsRequestDto;
import com.marketplace.newsservice.dto.response.NewsDetailDto;
import com.marketplace.newsservice.dto.response.NewsResponseDto;
import com.marketplace.newsservice.entity.News;
import com.marketplace.newsservice.exception.DuplicateSlugException;
import com.marketplace.newsservice.exception.NewsNotFoundException;
import com.marketplace.newsservice.mapper.NewsMapper;
import com.marketplace.newsservice.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;

    @Transactional
    public NewsDetailDto createNews(NewsRequestDto requestDto, UUID adminId) {
        log.info("Creating news article by admin: {}", adminId);

        if (requestDto.getSlug() != null && newsRepository.existsBySlugAndDeletedFalse(requestDto.getSlug())) {
            throw new DuplicateSlugException("News with slug '" + requestDto.getSlug() + "' already exists");
        }

        News news = newsMapper.toEntity(requestDto);
        news.setCreatedBy(adminId);

        if (Boolean.TRUE.equals(requestDto.getIsPublished()) && news.getPublishedAt() == null) {
            news.setPublishedAt(LocalDateTime.now());
        }

        News savedNews = newsRepository.save(news);
        log.info("News article created successfully: id={}, slug={}", savedNews.getId(), savedNews.getSlug());

        return newsMapper.toDetailDto(savedNews);
    }

    @Transactional(readOnly = true)
    public Page<NewsResponseDto> getPublishedNews(Pageable pageable) {
        log.debug("Fetching published news, page: {}", pageable.getPageNumber());
        return newsRepository.findByIsPublishedTrueAndDeletedFalseOrderByPublishedAtDesc(pageable)
                .map(newsMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<NewsResponseDto> getAllNews(Pageable pageable) {
        log.debug("Fetching all news (admin), page: {}", pageable.getPageNumber());
        return newsRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable)
                .map(newsMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<NewsResponseDto> getNewsByPublishedStatus(Boolean isPublished, Pageable pageable) {
        log.debug("Fetching news by published status: {}, page: {}", isPublished, pageable.getPageNumber());
        return newsRepository.findAllByPublishedStatus(isPublished, pageable)
                .map(newsMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public NewsDetailDto getNewsBySlug(String slug) {
        log.debug("Fetching published news by slug: {}", slug);
        News news = newsRepository.findBySlugAndIsPublishedTrueAndDeletedFalse(slug)
                .orElseThrow(() -> new NewsNotFoundException("Published news not found with slug: " + slug));
        return newsMapper.toDetailDto(news);
    }

    @Transactional(readOnly = true)
    public NewsDetailDto getNewsBySlugAdmin(String slug) {
        log.debug("Fetching news by slug (admin): {}", slug);
        News news = newsRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new NewsNotFoundException("News not found with slug: " + slug));
        return newsMapper.toDetailDto(news);
    }

    @Transactional(readOnly = true)
    public NewsDetailDto getNewsById(UUID id) {
        log.debug("Fetching news by id: {}", id);
        News news = newsRepository.findById(id)
                .filter(n -> !n.getDeleted())
                .orElseThrow(() -> new NewsNotFoundException("News not found with id: " + id));
        return newsMapper.toDetailDto(news);
    }

    @Transactional
    public NewsDetailDto updateNews(UUID id, NewsRequestDto requestDto) {
        log.info("Updating news article: {}", id);

        News news = newsRepository.findById(id)
                .filter(n -> !n.getDeleted())
                .orElseThrow(() -> new NewsNotFoundException("News not found with id: " + id));

        if (requestDto.getSlug() != null && 
            !requestDto.getSlug().equals(news.getSlug()) &&
            newsRepository.existsBySlugAndIdNotAndDeletedFalse(requestDto.getSlug(), id)) {
            throw new DuplicateSlugException("News with slug '" + requestDto.getSlug() + "' already exists");
        }

        boolean wasUnpublished = !news.getIsPublished();
        newsMapper.updateEntityFromDto(requestDto, news);

        if (Boolean.TRUE.equals(requestDto.getIsPublished()) && wasUnpublished) {
            news.setPublishedAt(LocalDateTime.now());
        } else if (Boolean.FALSE.equals(requestDto.getIsPublished())) {
            news.setPublishedAt(null);
        }

        News updatedNews = newsRepository.save(news);
        log.info("News article updated successfully: id={}", id);

        return newsMapper.toDetailDto(updatedNews);
    }

    @Transactional
    public void deleteNews(UUID id) {
        log.info("Soft deleting news article: {}", id);

        News news = newsRepository.findById(id)
                .filter(n -> !n.getDeleted())
                .orElseThrow(() -> new NewsNotFoundException("News not found with id: " + id));

        news.setDeleted(true);
        newsRepository.save(news);

        log.info("News article soft deleted successfully: id={}", id);
    }

    @Transactional
    public void permanentlyDeleteNews(UUID id) {
        log.info("Permanently deleting news article: {}", id);

        if (!newsRepository.existsById(id)) {
            throw new NewsNotFoundException("News not found with id: " + id);
        }

        newsRepository.deleteById(id);
        log.info("News article permanently deleted: id={}", id);
    }
}
