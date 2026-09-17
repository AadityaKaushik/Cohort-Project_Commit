package com.cohort.api.exception;

/** 409 — profile update tried to change email to one already taken. */
public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException() {
        super("That email address is already in use.");
    }
}
