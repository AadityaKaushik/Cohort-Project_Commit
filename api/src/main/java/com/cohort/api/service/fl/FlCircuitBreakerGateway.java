package com.cohort.api.service.fl;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FlCircuitBreakerGateway {
    private static final Logger log = LoggerFactory.getLogger(FlCircuitBreakerGateway.class);
    static final String CIRCUIT_BREAKER_NAME = "flService";

    private final FederatedLearningGateway delegate;
    private final CircuitBreaker circuitBreaker;

    public FlCircuitBreakerGateway(FederatedLearningGateway delegate, CircuitBreakerRegistry registry) {
        this.delegate = delegate;
        this.circuitBreaker = registry.circuitBreaker(CIRCUIT_BREAKER_NAME);
    }

    public Optional<FlModelInfo> fetchCurrentModelSafely() {
        Supplier<FlModelInfo> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, delegate::fetchCurrentModel);
        try {
            return Optional.of(decorated.get());
        } catch (Exception ex) {
            log.warn("fl-service unavailable ({}); falling back to local-only recommendations", ex.getMessage());
            return Optional.empty();
        }
    }
}
