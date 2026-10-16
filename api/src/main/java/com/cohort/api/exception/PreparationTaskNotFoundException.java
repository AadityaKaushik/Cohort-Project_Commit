package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class PreparationTaskNotFoundException extends RuntimeException {

    public PreparationTaskNotFoundException() {
        super("Preparation task not found.");
    }
}
