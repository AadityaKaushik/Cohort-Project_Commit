package com.cohort.api.exception;

/** Missing or made by someone else: 404, never 403. */
public class ContributionNotFoundException extends RuntimeException {

    public ContributionNotFoundException() {
        super("Contribution not found.");
    }
}
