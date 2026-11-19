package com.securefileshare.auth;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class BaseAuthIntegrationTest {

    protected static final String TEST_JWT_SECRET = "super_secret_test_jwt_key_auth_service_2026_at_least_32_chars!";

    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("sfs_auth")
            .withUsername("sfs_admin")
            .withPassword("sfs_secure_pass_2026");

    @Container
    protected static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = System.getenv().containsKey("CI")
                ? "jdbc:postgresql://localhost:5432/sfs_auth"
                : postgres.getJdbcUrl().replace("localhost", "127.0.0.1");

        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> System.getenv().containsKey("CI") ? "sfs_admin" : postgres.getUsername());
        registry.add("spring.datasource.password", () -> System.getenv().containsKey("CI") ? "sfs_secure_pass_2026" : postgres.getPassword());
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        String redisHost = System.getenv().containsKey("CI") ? "localhost" : "127.0.0.1";
        int redisPort = System.getenv().containsKey("CI") ? 6379 : redis.getMappedPort(6379);

        registry.add("spring.data.redis.host", () -> redisHost);
        registry.add("spring.data.redis.port", () -> redisPort);

        registry.add("security.jwt.secret", () -> TEST_JWT_SECRET);
    }
}
