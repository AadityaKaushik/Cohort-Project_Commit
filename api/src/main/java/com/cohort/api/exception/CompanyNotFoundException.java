package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class CompanyNotFoundException extends RuntimeException {

    public CompanyNotFoundException() {
        super("Company not found.");
    }
}
