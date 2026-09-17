package com.cohort.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared base for tests that need a real Postgres 16 instance (migrations,
 * repositories, full auth flows) rather than mocks — per the project's
 * testing philosophy, anything that touches real Postgres uses
 * Testcontainers instead of an in-memory substitute.
 *
 * Subclasses must add {@code @Import(AbstractIntegrationTest.ContainerConfig.class)}
 * — Spring Boot only auto-detects a nested {@code @TestConfiguration} on the
 * test class itself, not on an inherited superclass.
 */
@Testcontainers
public abstract class AbstractIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    public static class ContainerConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return new PostgreSQLContainer<>("postgres:16-alpine");
        }
    }
}
