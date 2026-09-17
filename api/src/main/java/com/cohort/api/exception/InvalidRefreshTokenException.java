package com.cohort.api.exception;

/** 401 — refresh token missing, expired, already revoked, or unknown. */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid or expired.");
    }
}
