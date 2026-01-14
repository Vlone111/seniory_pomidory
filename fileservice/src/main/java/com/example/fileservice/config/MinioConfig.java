package com.example.fileservice.config;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MinioConfig {
    
    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${minio.port}")
    private int minioPort;

    @Value("${minio.access-key}")
    private String minioAccessKey;

    @Value("${minio.secret-key}")
    private String minioSecretKey;

    @Value("${minio.secure:false}")
    private boolean secure;

    @Bean
    public MinioClient minioClient() {
        try {
            log.info("Initializing MinIO client: {}:{}", minioEndpoint, minioPort);
            return MinioClient.builder()
                    .endpoint(minioEndpoint, minioPort, secure)
                    .credentials(minioAccessKey, minioSecretKey)
                    .build();
        } catch (Exception e) {
            log.error("Failed to initialize MinIO client", e);
            throw new RuntimeException("Failed to initialize MinIO client", e);
        }
    }
}
