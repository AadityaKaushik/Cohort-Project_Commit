package com.cohort.api.exception;

public class DuplicateOfferException extends RuntimeException {

    public DuplicateOfferException() {
        super("This application already has an offer.");
    }
}
