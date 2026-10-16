package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class InterviewNotFoundException extends RuntimeException {

    public InterviewNotFoundException() {
        super("Interview not found.");
    }
}
