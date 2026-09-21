package com.cohort.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The one {@link RestClient} every fl-service caller shares (P51's export
 * job, P52's {@code FlowerServiceAdapter}). Short, deliberately-configured
 * timeouts matter here specifically because P53 wraps calls through this
 * client in a Resilience4j circuit breaker with a local-only fallback — a
 * slow-but-not-quite-timed-out fl-service must still fail fast enough that
 * the Recommendations page never hangs waiting on it.
 */
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
