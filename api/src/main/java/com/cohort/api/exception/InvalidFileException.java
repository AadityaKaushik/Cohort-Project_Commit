package com.cohort.api.exception;

/** Thrown by any stage of the resume-upload validation chain (US-21) — maps to 415 Unsupported Media Type. */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
