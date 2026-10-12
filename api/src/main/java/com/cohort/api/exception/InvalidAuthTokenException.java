package com.cohort.api.exception;

/** 400 — reset or verification token unknown, expired, or already used. */
public class InvalidAuthTokenException extends RuntimeException {

    public InvalidAuthTokenException() {
        super("This link is invalid or has expired.");
    }
}
