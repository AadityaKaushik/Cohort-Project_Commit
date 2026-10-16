package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException() {
        super("Application not found.");
    }
}
