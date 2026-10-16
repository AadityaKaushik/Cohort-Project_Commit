package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class OfferNotFoundException extends RuntimeException {

    public OfferNotFoundException() {
        super("Offer not found.");
    }
}
