package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class ContactNotFoundException extends RuntimeException {

    public ContactNotFoundException() {
        super("Contact not found.");
    }
}
