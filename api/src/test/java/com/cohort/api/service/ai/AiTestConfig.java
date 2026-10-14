package com.cohort.api.service.ai;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class AiTestConfig {
    @Bean
    @Primary
    public StubAiProvider stubAiProvider() {
        return new StubAiProvider();
    }
}
