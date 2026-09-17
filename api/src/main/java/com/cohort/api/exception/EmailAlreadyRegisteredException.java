package com.cohort.api.exception;

/** 409 — the requested email is already registered. */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("Unable to complete registration with the supplied details.");
    }
}
