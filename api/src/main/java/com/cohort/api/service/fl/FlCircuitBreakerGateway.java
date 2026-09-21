package com.cohort.api.service.fl;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Circuit Breaker pattern, not GoF (ARCHITECTURE_REVISION_JAVA.md §4.1,
 * new in the architecture revision): wraps {@link FederatedLearningGateway}
 * (P52's Adapter) with a Resilience4j circuit breaker whose only job is
 * turning "fl-service is slow, down, or mid-deploy" into "no federated
 * signal available this request" — never an exception the caller has to
 * handle. {@code RecommendationFacade}/the Federated Lift viewer (P57) call
 * only this class, never {@link FlowerServiceAdapter} directly, which is
 * what makes the fallback impossible to accidentally bypass.
 *
 * <p>Used programmatically ({@code CircuitBreaker.decorateSupplier}) rather
 * than via {@code @CircuitBreaker} annotations: the fallback-to-empty
 * behaviour is then one visible method here instead of depending on Spring
 * AOP proxying a self-invocation correctly, and it is trivially unit
 * testable by constructing a real {@link CircuitBreakerRegistry} in a test
 * with no Spring context at all.
 */
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

    /**
     * US-65 / the architecture revision's own framing: "if the FL service is
     * unreachable, the API silently serves local-only recommendations."
     * Never throws — any failure (the adapter's own
     * {@link FlServiceUnavailableException}, the breaker being OPEN, or
     * anything else) becomes {@link Optional#empty()}, which every caller
     * treats as "proceed with local-only ranking."
     */
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
