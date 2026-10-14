package com.cohort.api.exception;

public class AiOutputInvalidException extends RuntimeException {
    public AiOutputInvalidException(String message) {
        super(message);
    }
}
