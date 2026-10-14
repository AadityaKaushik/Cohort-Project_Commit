package com.cohort.api.service.notification;

public interface NotificationChannel {
    String id();

    // Delivers {@code message}; must be idempotent per {@link NotificationMessage#dedupeKey()}.
    boolean deliver(NotificationMessage message);
}
