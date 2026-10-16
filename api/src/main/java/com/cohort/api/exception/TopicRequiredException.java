package com.cohort.api.exception;

public class TopicRequiredException extends RuntimeException {

    public TopicRequiredException() {
        super("Pick a topic for this question.");
    }
}
