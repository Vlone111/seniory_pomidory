package com.example.fileservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponseDto {

    private String fileUrl;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String contentType;
    private String category;
    private String thumbnailUrl;
    private LocalDateTime uploadedAt;
}
