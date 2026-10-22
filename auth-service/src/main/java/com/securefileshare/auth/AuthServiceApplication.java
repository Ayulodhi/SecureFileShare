package com.securefileshare.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application Entrypoint for Auth Service.
 * WHAT: Bootstraps the user identity, cryptographic key registry, and JWT microservice.
 * WHY: Serves as the central security and authentication authority for SecureFileShare.
 */
@SpringBootApplication
public class AuthServiceApplication {

    /**
     * Standard main method launching the Spring Boot context.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
