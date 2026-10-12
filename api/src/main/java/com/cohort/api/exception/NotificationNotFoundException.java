package com.cohort.api.exception;

/** Thrown for both a missing notification ID and one belonging to another user — FR-95: always 404, never 403. */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException() {
        super("Notification not found.");
    }
}
