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
        // The breaker is now open: one more call returns empty instead of throwing.
        assertThat(gateway.fetchCurrentModelSafely()).isEmpty();
        assertThat(registry.circuitBreaker(FlCircuitBreakerGateway.CIRCUIT_BREAKER_NAME).getState())
                .isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
    }

    private CircuitBreakerRegistry freshRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
}
