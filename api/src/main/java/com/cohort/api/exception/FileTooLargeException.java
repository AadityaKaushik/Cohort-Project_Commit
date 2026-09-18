package com.cohort.api.exception;

/** Thrown when an uploaded resume exceeds the 5 MB cap (FR-30) — maps to 413 Payload Too Large. */
public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException(String message) {
        super(message);
    }
}
