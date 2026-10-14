package com.cohort.api.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("Unable to complete registration with the supplied details.");
    }
}
