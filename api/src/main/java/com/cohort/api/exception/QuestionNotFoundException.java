package com.cohort.api.exception;

/** Thrown for a missing question ID, one belonging to another user, or a system question addressed by a write endpoint — always 404, never 403 (FR-95). */
public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException() {
        super("Question not found.");
    }
}
