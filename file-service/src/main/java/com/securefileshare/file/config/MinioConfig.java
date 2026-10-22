package com.securefileshare.file.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private final String bucketName;

    public MinioConfig(
            @Value("${minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${minio.access-key:minio_admin}") String accessKey,
            @Value("${minio.secret-key}") String secretKey,
            @Value("${minio.bucket-name:secure-files}") String bucketName) {
        // Enforce secret presence without default fallbacks
        // Why: Prevents running object storage with unauthenticated or default credentials
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("Missing required MinIO secret key. Provide MINIO_ROOT_PASSWORD environment variable.");
        }
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
    }

    /**
     * Creates and initializes the MinioClient Spring bean.
     * WHAT: Connects to MinIO and creates the configured bucket if it does not yet exist.
     * WHY: Ensures object storage is ready before the application starts accepting file upload streams.
     *
     * @return Initialized MinioClient bean
     */
    @Bean
    public MinioClient minioClient() {
        log.info("Connecting to MinIO endpoint: {} with bucket: {}", endpoint, bucketName);
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // Ensure the target storage bucket exists upon startup
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                log.info("Bucket '{}' does not exist. Creating bucket...", bucketName);
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket '{}' created successfully.", bucketName);
            }
        } catch (Exception ex) {
            log.warn("Could not verify or create MinIO bucket on startup (will retry on operations): {}", ex.getMessage());
        }

        return client;
    }
}
