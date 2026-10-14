package com.cohort.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class FlServiceConfig {
    @Bean
    public RestClient flServiceRestClient(
            RestClient.Builder builder,
            @Value("${app.fl.service-base-url}") String baseUrl,
            @Value("${app.fl.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${app.fl.read-timeout-ms:3000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
