package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class PreparationTaskNotFoundException extends RuntimeException {

    public PreparationTaskNotFoundException() {
        super("Preparation task not found.");
    }
}
