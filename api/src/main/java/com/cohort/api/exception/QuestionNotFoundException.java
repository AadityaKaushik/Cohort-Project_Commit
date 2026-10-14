package com.cohort.api.exception;

/** Missing, owned by another user, or a system question on a write endpoint: 404, never 403. */
public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException() {
        super("Question not found.");
    }
}
