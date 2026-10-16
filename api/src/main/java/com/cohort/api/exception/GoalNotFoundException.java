package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class GoalNotFoundException extends RuntimeException {

    public GoalNotFoundException() {
        super("Goal not found.");
    }
}
