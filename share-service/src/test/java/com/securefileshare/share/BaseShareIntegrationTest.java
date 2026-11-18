package com.securefileshare.share;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class BaseShareIntegrationTest {

    protected static final String TEST_JWT_SECRET = "super_secret_test_jwt_key_share_service_2026_at_least_32_chars!";

    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("sfs_share")
            .withUsername("sfs_admin")
            .withPassword("sfs_secure_pass_2026");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl().replace("localhost", "127.0.0.1"));
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("security.jwt.secret", () -> TEST_JWT_SECRET);
    }
}
