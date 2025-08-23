package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Auth Spring Boot application.
 * <p>
 * This class bootstraps the application using {@link SpringApplication}.
 * It also enables scheduling tasks using {@link EnableScheduling}.
 * </p>
 *
 * <p>
 * Unit testing should verify that the application context loads successfully.
 * </p>
 */
@SpringBootApplication
@EnableScheduling
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
