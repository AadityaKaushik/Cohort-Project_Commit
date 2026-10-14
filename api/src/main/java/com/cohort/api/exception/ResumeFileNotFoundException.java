package com.cohort.api.exception;

public class ResumeFileNotFoundException extends RuntimeException {
    public ResumeFileNotFoundException() {
        super("This resume has no file uploaded yet.");
    }
}
