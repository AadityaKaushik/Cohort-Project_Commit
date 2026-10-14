package com.cohort.api.exception;

public class InvalidImportException extends RuntimeException {
    public InvalidImportException(String message) {
        super(message);
    }
}
