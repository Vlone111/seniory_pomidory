package com.example.fileservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioInitializer {

    private final MinioClient minioClient;
    
    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.init.retry-count:3}")
    private int retryCount;

    @Value("${minio.init.retry-delay:5000}")
    private long retryDelay;

    @PostConstruct
    public void init() {
        initWithRetry();
    }

    private void initWithRetry() {
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                setupBucketAndPolicy();
                return;
            } catch (Exception e) {
                log.warn("[MinIO] Initialization attempt {}/{} failed: {}", attempt, retryCount, e.getMessage());
                if (attempt < retryCount) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("[MinIO] All initialization attempts failed. Service will start but MinIO may not be ready.");
                }
            }
        }
    }

    public void setupBucketAndPolicy() {
        try {
            boolean found = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );

            if (!found) {
                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                );
                log.info("[MinIO] Bucket '{}' created successfully", bucketName);
            } else {
                log.info("[MinIO] Bucket '{}' already exists", bucketName);
            }

            String publicReadPolicy = String.format(
                    "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\","
                            + "\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::%s/*\"]}]}",
                    bucketName
            );

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(publicReadPolicy)
                            .build()
            );
            
            log.info("[MinIO] Public read policy applied to bucket '{}'", bucketName);

        } catch (Exception e) {
            log.error("[MinIO] Setup failed: {}", e.getMessage());
            throw new RuntimeException("MinIO setup failed", e);
        }
    }
}