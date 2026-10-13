package com.cohort.api.exception;

/** Thrown for both a missing note ID and one belonging to another user: always 404, never 403. */
public class CompanyNoteNotFoundException extends RuntimeException {

    public CompanyNoteNotFoundException() {
        super("Company note not found.");
    }
}
