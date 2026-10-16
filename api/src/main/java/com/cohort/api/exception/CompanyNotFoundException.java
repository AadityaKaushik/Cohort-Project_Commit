package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class CompanyNotFoundException extends RuntimeException {

    public CompanyNotFoundException() {
        super("Company not found.");
    }
}
