package com.cohort.api.service.fl;

/**
 * Wraps any failure talking to fl-service (connection refused, timeout, a
 * non-2xx response) into one exception type — what {@link FlowerServiceAdapter}
 * throws and what P53's circuit breaker is configured to treat as "fl-service
 * is unavailable, fall back to local-only."
 */
public class FlServiceUnavailableException extends RuntimeException {

    public FlServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
