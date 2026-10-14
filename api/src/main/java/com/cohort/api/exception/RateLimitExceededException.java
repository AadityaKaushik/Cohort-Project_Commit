package com.cohort.api.exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super("Too many login attempts. Try again later.");
    }
}
