package com.securefileshare.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application Entrypoint for File Service.
 * WHAT: Bootstraps the file management and encrypted object storage microservice.
 * WHY: Operates as an independent microservice handling MinIO ciphertext streaming.
 */
@SpringBootApplication
public class FileServiceApplication {

    /**
     * Standard main method launching the Spring Boot context.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
    }
}
