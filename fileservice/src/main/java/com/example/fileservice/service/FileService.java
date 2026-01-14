package com.example.fileservice.service;

import com.example.fileservice.dto.response.FileInfoDto;
import com.example.fileservice.dto.response.FileUploadResponseDto;
import com.example.fileservice.exception.FileNotFoundException;
import com.example.fileservice.exception.FileStorageException;
import com.example.fileservice.exception.InvalidFileException;
import com.example.fileservice.model.FileCategory;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.public-url}")
    private String publicUrl;

    @Value("${file.max-size:10485760}") // 10MB default
    private long maxFileSize;

    @Value("${file.allowed-extensions}")
    private String allowedExtensions;

    @Value("${file.thumbnail.width:200}")
    private int thumbnailWidth;

    @Value("${file.thumbnail.height:200}")
    private int thumbnailHeight;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    public FileUploadResponseDto uploadFile(MultipartFile file) {
        return uploadFile(file, FileCategory.GENERAL);
    }

    public FileUploadResponseDto uploadFile(MultipartFile file, FileCategory category) {
        log.info("Starting file upload: {} to category: {}", file.getOriginalFilename(), category);

        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueId = UUID.randomUUID().toString();
        String objectName = category.getPrefix() + "/" + uniqueId + fileExtension;

        try {
            // Upload original file
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();

            minioClient.putObject(args);

            String fileUrl = String.format("%s/%s/%s", publicUrl, bucketName, objectName);
            String thumbnailUrl = null;

            // Generate thumbnail for images
            if (isImageFile(fileExtension) && shouldGenerateThumbnail(category)) {
                thumbnailUrl = generateAndUploadThumbnail(file, category, uniqueId, fileExtension);
            }

            log.info("File uploaded successfully: {} -> {}", originalFilename, objectName);

            return FileUploadResponseDto.builder()
                    .fileUrl(fileUrl)
                    .fileName(objectName)
                    .originalFileName(originalFilename)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .category(category.name())
                    .thumbnailUrl(thumbnailUrl)
                    .uploadedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to upload file: {}", originalFilename, e);
            throw new FileStorageException("Failed to upload file: " + originalFilename, e);
        }
    }

    public List<FileUploadResponseDto> uploadFiles(List<MultipartFile> files, FileCategory category) {
        log.info("Starting batch upload of {} files to category: {}", files.size(), category);
        
        return files.stream()
                .map(file -> uploadFile(file, category))
                .toList();
    }

    public CompletableFuture<FileUploadResponseDto> uploadFileAsync(MultipartFile file, FileCategory category) {
        return CompletableFuture.supplyAsync(() -> uploadFile(file, category));
    }

    private String generateAndUploadThumbnail(MultipartFile file, FileCategory category, 
                                               String uniqueId, String fileExtension) {
        try {
            ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream();
            
            Thumbnails.of(file.getInputStream())
                    .size(thumbnailWidth, thumbnailHeight)
                    .keepAspectRatio(true)
                    .outputFormat(fileExtension.replace(".", ""))
                    .toOutputStream(thumbnailOutputStream);

            byte[] thumbnailBytes = thumbnailOutputStream.toByteArray();
            String thumbnailObjectName = getThumbnailPrefix(category) + "/" + uniqueId + "_thumb" + fileExtension;

            PutObjectArgs thumbnailArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(thumbnailObjectName)
                    .stream(new ByteArrayInputStream(thumbnailBytes), thumbnailBytes.length, -1)
                    .contentType(file.getContentType())
                    .build();

            minioClient.putObject(thumbnailArgs);

            log.info("Thumbnail generated: {}", thumbnailObjectName);
            return String.format("%s/%s/%s", publicUrl, bucketName, thumbnailObjectName);

        } catch (Exception e) {
            log.warn("Failed to generate thumbnail for file, continuing without thumbnail", e);
            return null;
        }
    }

    private String getThumbnailPrefix(FileCategory category) {
        return switch (category) {
            case SHOP_AVATAR -> "shops/avatars/thumbnails";
            case SHOP_BANNER -> "shops/banners/thumbnails";
            case PRODUCT_IMAGE -> "products/thumbnails";
            case USER_AVATAR -> "users/avatars/thumbnails";
            default -> "general/thumbnails";
        };
    }

    private boolean shouldGenerateThumbnail(FileCategory category) {
        return category == FileCategory.SHOP_AVATAR || 
               category == FileCategory.PRODUCT_IMAGE || 
               category == FileCategory.USER_AVATAR;
    }

    private boolean isImageFile(String extension) {
        return IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    public InputStream getFile(String fileName) {
        log.info("Retrieving file: {}", fileName);

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to retrieve file: {}", fileName, e);
            throw new FileNotFoundException("File not found: " + fileName);
        }
    }

    public void deleteFile(String fileName) {
        log.info("Deleting file: {}", fileName);

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            log.info("File deleted successfully: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to delete file: {}", fileName, e);
            throw new FileStorageException("Failed to delete file: " + fileName, e);
        }
    }

    public List<FileInfoDto> listFiles() {
        return listFiles(null);
    }

    public List<FileInfoDto> listFiles(FileCategory category) {
        String prefix = category != null ? category.getPrefix() + "/" : null;
        log.info("Listing files in bucket: {}, prefix: {}", bucketName, prefix);

        List<FileInfoDto> files = new ArrayList<>();

        try {
            ListObjectsArgs.Builder argsBuilder = ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .recursive(true);
            
            if (prefix != null) {
                argsBuilder.prefix(prefix);
            }

            Iterable<Result<Item>> results = minioClient.listObjects(argsBuilder.build());

            for (Result<Item> result : results) {
                Item item = result.get();
                // Skip thumbnail files in listing
                if (item.objectName().contains("/thumbnails/")) {
                    continue;
                }
                
                String fileUrl = String.format("%s/%s/%s", publicUrl, bucketName, item.objectName());

                files.add(FileInfoDto.builder()
                        .fileName(item.objectName())
                        .size(item.size())
                        .contentType("application/octet-stream")
                        .url(fileUrl)
                        .build());
            }

            log.info("Found {} files", files.size());
            return files;

        } catch (Exception e) {
            log.error("Failed to list files", e);
            throw new FileStorageException("Failed to list files", e);
        }
    }

    public String getFileUrl(String fileName) {
        if (fileExists(fileName)) {
            return String.format("%s/%s/%s", publicUrl, bucketName, fileName);
        }
        throw new FileNotFoundException("File not found: " + fileName);
    }

    public boolean fileExists(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new InvalidFileException(
                    String.format("File size exceeds maximum allowed size of %d bytes", maxFileSize)
            );
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidFileException("File name is invalid");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        Set<String> allowedExtensionsSet = new HashSet<>(Arrays.asList(allowedExtensions.split(",")));

        if (!allowedExtensionsSet.contains(extension)) {
            throw new InvalidFileException(
                    String.format("File extension '%s' is not allowed. Allowed: %s", 
                            extension, allowedExtensions)
            );
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}