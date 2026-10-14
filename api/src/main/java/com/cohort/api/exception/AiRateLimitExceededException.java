package com.cohort.api.exception;

public class AiRateLimitExceededException extends RuntimeException {
    public AiRateLimitExceededException() {
        super("Too many AI requests. Try again later.");
    }
}
