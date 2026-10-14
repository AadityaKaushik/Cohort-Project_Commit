package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class OfferNotFoundException extends RuntimeException {

    public OfferNotFoundException() {
        super("Offer not found.");
    }
}
