package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class InterviewQuestionNotFoundException extends RuntimeException {

    public InterviewQuestionNotFoundException() {
        super("Interview question not found.");
    }
}
