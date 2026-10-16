package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class ContributionNotFoundException extends RuntimeException {

    public ContributionNotFoundException() {
        super("Contribution not found.");
    }
}
