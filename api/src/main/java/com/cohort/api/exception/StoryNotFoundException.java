package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class StoryNotFoundException extends RuntimeException {

    public StoryNotFoundException() {
        super("Story not found.");
    }
}
