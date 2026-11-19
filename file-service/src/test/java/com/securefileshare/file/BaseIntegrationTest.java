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
    protected static final GenericContainer<?> minio = new GenericContainer<>(
            new org.testcontainers.images.builder.ImageFromDockerfile("sfs-minio-test:latest")
                    .withFileFromClasspath("Dockerfile", "docker/minio/Dockerfile")
    )
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minio_admin")
            .withEnv("MINIO_ROOT_PASSWORD", "minio_secure_pass_2026")
            .withEnv("MINIO_ACCESS_KEY", "minio_admin")
            .withEnv("MINIO_SECRET_KEY", "minio_secure_pass_2026")
            .withCommand("server", "/data", "--console-address", ":9001");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = System.getenv().containsKey("CI")
                ? "jdbc:postgresql://localhost:5432/sfs_file"
                : postgres.getJdbcUrl().replace("localhost", "127.0.0.1");

        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> System.getenv().containsKey("CI") ? "sfs_admin" : postgres.getUsername());
        registry.add("spring.datasource.password", () -> System.getenv().containsKey("CI") ? "sfs_secure_pass_2026" : postgres.getPassword());
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
