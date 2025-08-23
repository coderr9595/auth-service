package com.example.auth;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {com.example.auth.AuthApplication.class, TestMailConfig.class})
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    @Container
    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("auth_db_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @BeforeAll
    void init() {
        boolean useLocal = Boolean.parseBoolean(Optional.ofNullable(System.getenv("TEST_USE_LOCAL_DB")).orElse("false"));
        if (!useLocal) {
            System.setProperty("spring.datasource.url", POSTGRES.getJdbcUrl());
            System.setProperty("spring.datasource.username", POSTGRES.getUsername());
            System.setProperty("spring.datasource.password", POSTGRES.getPassword());
        }
        System.setProperty("jwt.secret", "test-secret-should-be-long-enough-to-derive-key-1234567890123456");
        System.setProperty("jwt.expirationMs", "60000");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "update");
    }
}


