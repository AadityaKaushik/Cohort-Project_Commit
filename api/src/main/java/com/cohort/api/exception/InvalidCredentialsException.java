package com.cohort.api.exception;

/** Same message for an unknown email and a wrong password, so the response does not say which. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password.");
    }
}
