package com.cohort.api.exception;

/** Thrown for both a missing contact ID and one belonging to another user: always 404, never 403. */
public class ContactNotFoundException extends RuntimeException {

    public ContactNotFoundException() {
        super("Contact not found.");
    }
}
