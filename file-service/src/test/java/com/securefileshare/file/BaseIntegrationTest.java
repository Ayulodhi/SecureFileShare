package com.securefileshare.file;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class BaseIntegrationTest {

    protected static final String TEST_JWT_SECRET = "super_secret_test_jwt_key_file_service_2026_at_least_32_chars!";

    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("sfs_file")
            .withUsername("sfs_admin")
            .withPassword("sfs_secure_pass_2026");

    @Container
    protected static final GenericContainer<?> minio = new GenericContainer<>("quay.io/minio/minio:latest")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minio_admin")
            .withEnv("MINIO_ROOT_PASSWORD", "minio_secure_pass_2026")
            .withCommand("server /data");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        String minioEndpoint = System.getenv().containsKey("CI")
                ? System.getenv().getOrDefault("MINIO_ENDPOINT", "http://localhost:9000")
                : "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);

        registry.add("minio.endpoint", () -> minioEndpoint);
        registry.add("minio.access-key", () -> "minio_admin");
        registry.add("minio.secret-key", () -> "minio_secure_pass_2026");
        registry.add("minio.bucket-name", () -> "secure-files");

        registry.add("security.jwt.secret", () -> TEST_JWT_SECRET);
    }
}
