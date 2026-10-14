package com.cohort.api.exception;

public class EmailAlreadyInUseException extends RuntimeException {
    public EmailAlreadyInUseException() {
        super("That email address is already in use.");
    }
}
