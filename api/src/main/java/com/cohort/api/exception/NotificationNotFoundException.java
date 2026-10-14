package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException() {
        super("Notification not found.");
    }
}
