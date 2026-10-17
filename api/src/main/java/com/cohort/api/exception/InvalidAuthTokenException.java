package com.cohort.api.exception;

public class InvalidAuthTokenException extends RuntimeException {
    public InvalidAuthTokenException() {
        super("That code is wrong or has expired.");
    }
}
