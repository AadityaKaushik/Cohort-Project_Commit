package com.cohort.api.exception;

public class DuplicateContributionException extends RuntimeException {

    public DuplicateContributionException() {
        super("You have already shared an experience for this company, role and year.");
    }
}
