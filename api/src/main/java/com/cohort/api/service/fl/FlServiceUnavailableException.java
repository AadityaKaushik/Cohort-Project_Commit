package com.cohort.api.service.fl;

public class FlServiceUnavailableException extends RuntimeException {
    public FlServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
