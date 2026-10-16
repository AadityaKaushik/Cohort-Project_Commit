package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class ResumeNotFoundException extends RuntimeException {

    public ResumeNotFoundException() {
        super("Resume not found.");
    }
}
