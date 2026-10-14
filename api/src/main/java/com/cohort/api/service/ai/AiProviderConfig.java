package com.cohort.api.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiProviderConfig {
    @Bean
    public AiProvider aiProvider(
            ObjectMapper mapper,
            @Value("${app.ai.provider}") String provider,
            @Value("${app.ai.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${app.ai.read-timeout-ms}") long readTimeoutMs,
            @Value("${app.ai.ollama.base-url}") String ollamaUrl,
            @Value("${app.ai.ollama.model}") String ollamaModel,
            @Value("${app.ai.gemini.base-url}") String geminiUrl,
            @Value("${app.ai.gemini.model}") String geminiModel,
            @Value("${app.ai.gemini.api-key}") String geminiKey) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build();
        Duration readTimeout = Duration.ofMillis(readTimeoutMs);
        if ("gemini".equalsIgnoreCase(provider) && !geminiKey.isBlank()) {
            return new GeminiProvider(client, mapper, geminiUrl, geminiModel, geminiKey, readTimeout);
        }
        return new OllamaProvider(client, mapper, ollamaUrl, ollamaModel, readTimeout);
    }
}
