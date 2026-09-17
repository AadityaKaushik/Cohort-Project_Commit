package com.cohort.api.exception;

/** 400 — password fails the length or common-password-blocklist policy. */
public class WeakPasswordException extends RuntimeException {

    public WeakPasswordException(String message) {
        super(message);
    }
}
