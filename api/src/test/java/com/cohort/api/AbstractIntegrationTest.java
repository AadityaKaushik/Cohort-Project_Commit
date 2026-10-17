package com.cohort.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
// Most suites register users and use them straight away; EmailCodeIntegrationTest covers the required path.
@TestPropertySource(properties = "app.auth.email-verification-required=false")
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
