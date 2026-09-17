package com.cohort.api.exception;

/**
 * 401 — wrong password or unknown email. Deliberately the same exception,
 * same message, for both cases (FR-02): a client cannot distinguish "no
 * such account" from "wrong password" from the response alone.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password.");
    }
}
