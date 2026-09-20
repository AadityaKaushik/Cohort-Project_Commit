package com.cohort.api.exception;

public class TopicNotFoundException extends RuntimeException {

    public TopicNotFoundException() {
        super("Topic not found.");
    }
}
