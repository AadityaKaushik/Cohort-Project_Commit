package com.cohort.api.service.fl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * P53 (Circuit Breaker): {@link FlCircuitBreakerGateway} must never let a
 * {@link FlServiceUnavailableException} from the underlying gateway escape
 * — it always turns into {@link Optional#empty()}. Uses a real
 * {@link CircuitBreakerRegistry} (not a mock of Resilience4j's own types)
 * so this test exercises the actual library behaviour, not an assumption
 * about it.
 */
class FlCircuitBreakerGatewayTest {

    @Test
    void returnsTheModelWrappedInOptionalWhenTheDelegateSucceeds() {
        FederatedLearningGateway delegate = mock(FederatedLearningGateway.class);
        FlModelInfo model = new FlModelInfo(1, 1, 3, Instant.now(), List.of(0.1, 0.2));
        when(delegate.fetchCurrentModel()).thenReturn(model);
        FlCircuitBreakerGateway gateway = new FlCircuitBreakerGateway(delegate, freshRegistry());

        Optional<FlModelInfo> result = gateway.fetchCurrentModelSafely();

        assertThat(result).contains(model);
    }

    @Test
    void returnsEmptyRatherThanThrowingWhenTheDelegateFails() {
        FederatedLearningGateway delegate = mock(FederatedLearningGateway.class);
        when(delegate.fetchCurrentModel()).thenThrow(new FlServiceUnavailableException("down", null));
        FlCircuitBreakerGateway gateway = new FlCircuitBreakerGateway(delegate, freshRegistry());

        Optional<FlModelInfo> result = gateway.fetchCurrentModelSafely();

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyOnceTheBreakerHasOpenedFromRepeatedFailures() {
        FederatedLearningGateway delegate = mock(FederatedLearningGateway.class);
        when(delegate.fetchCurrentModel()).thenThrow(new FlServiceUnavailableException("down", null));
        // A tiny window/threshold so a handful of calls is enough to trip the breaker in this test.
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(java.time.Duration.ofMinutes(1))
                .recordException(ex -> ex instanceof FlServiceUnavailableException)
                .build());
        FlCircuitBreakerGateway gateway = new FlCircuitBreakerGateway(delegate, registry);

        for (int i = 0; i < 4; i++) {
            assertThat(gateway.fetchCurrentModelSafely()).isEmpty();
        }
        // The breaker should now be OPEN -- one more call must still come back empty (via CallNotPermittedException), not throw.
        assertThat(gateway.fetchCurrentModelSafely()).isEmpty();
        assertThat(registry.circuitBreaker(FlCircuitBreakerGateway.CIRCUIT_BREAKER_NAME).getState())
                .isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
    }

    private CircuitBreakerRegistry freshRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
}
