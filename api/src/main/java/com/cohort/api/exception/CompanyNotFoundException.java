package com.cohort.api.exception;

/** Thrown for both a missing company ID and one belonging to another user: always 404, never 403. */
public class CompanyNotFoundException extends RuntimeException {

    public CompanyNotFoundException() {
        super("Company not found.");
    }
}
