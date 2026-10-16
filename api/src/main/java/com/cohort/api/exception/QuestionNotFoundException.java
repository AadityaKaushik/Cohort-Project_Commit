package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException() {
        super("Question not found.");
    }
}
