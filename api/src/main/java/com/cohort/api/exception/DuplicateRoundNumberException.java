package com.cohort.api.exception;

/** US-27: {@code round_number} must be unique within an application (V9 migration's unique constraint). */
public class DuplicateRoundNumberException extends RuntimeException {

    public DuplicateRoundNumberException(short roundNumber) {
        super("Round " + roundNumber + " already exists for this application.");
    }
}
