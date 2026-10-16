package com.cohort.api.exception;

/** US-27: {@code round_number} is unique within an application. */
public class DuplicateRoundNumberException extends RuntimeException {

    public DuplicateRoundNumberException(short roundNumber) {
        super("Round " + roundNumber + " already exists for this application.");
    }
}
