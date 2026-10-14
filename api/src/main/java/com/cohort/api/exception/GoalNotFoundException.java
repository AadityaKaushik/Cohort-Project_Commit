package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class GoalNotFoundException extends RuntimeException {

    public GoalNotFoundException() {
        super("Goal not found.");
    }
}
