package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException() {
        super("Notification not found.");
    }
}
