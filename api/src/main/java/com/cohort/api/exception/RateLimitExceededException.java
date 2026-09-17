package com.cohort.api.exception;

/** 429 — too many login attempts for this account in the current window. */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Too many login attempts. Try again later.");
    }
}
