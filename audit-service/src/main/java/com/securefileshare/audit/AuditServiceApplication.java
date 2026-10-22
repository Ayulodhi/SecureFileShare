package com.securefileshare.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application Entrypoint for Audit Service.
 * WHAT: Bootstraps the audit logging and security event tracking microservice.
 * WHY: Records critical authorization, key registration, and download audit trails.
 */
@SpringBootApplication
public class AuditServiceApplication {

    /**
     * Standard main method launching the Spring Boot context.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
