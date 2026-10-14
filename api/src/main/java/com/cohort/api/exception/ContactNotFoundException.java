package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class ContactNotFoundException extends RuntimeException {

    public ContactNotFoundException() {
        super("Contact not found.");
    }
}
