package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class CompanyNoteNotFoundException extends RuntimeException {

    public CompanyNoteNotFoundException() {
        super("Company note not found.");
    }
}
