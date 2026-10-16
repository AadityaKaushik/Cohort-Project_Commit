package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class ResumeSectionNotFoundException extends RuntimeException {

    public ResumeSectionNotFoundException() {
        super("Resume section not found.");
    }
}
