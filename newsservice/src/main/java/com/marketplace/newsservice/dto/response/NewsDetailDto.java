package com.marketplace.newsservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsDetailDto {

    private UUID id;
    private String title;
    private String content;
    private String slug;
    private String previewImageUrl;
    private Boolean isPublished;
    private LocalDateTime publishedAt;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
