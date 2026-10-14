package com.cohort.api.exception;

public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException() {
        super("AI unavailable");
    }

    public AiUnavailableException(Throwable cause) {
        super("AI unavailable", cause);
    }
}
