package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class StoryNotFoundException extends RuntimeException {

    public StoryNotFoundException() {
        super("Story not found.");
    }
}
