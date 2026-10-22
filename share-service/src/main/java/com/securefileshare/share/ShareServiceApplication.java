package com.securefileshare.share;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application Entrypoint for Share Service.
 * WHAT: Bootstraps the cryptographic share management microservice.
 * WHY: Operates independently to manage HMAC tokens, permissions, and revocation states.
 */
@SpringBootApplication
public class ShareServiceApplication {

    /**
     * Standard main method launching the Spring Boot context.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ShareServiceApplication.class, args);
    }
}
