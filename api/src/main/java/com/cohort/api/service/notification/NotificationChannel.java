package com.cohort.api.service.notification;

/**
 * Strategy pattern (ARCHITECTURE_REVISION_JAVA.md §4.1, NFR-53): one way of
 * delivering a {@link NotificationMessage} to its user. In-app is the only
 * strategy today ({@link InAppNotificationChannel}); email or push would be
 * new implementations of this interface, picked up by
 * {@link NotificationService} through configuration, with no change to the
 * listener, the service's dispatch logic, or -- above all -- any detector.
 */
public interface NotificationChannel {

    /** Stable identifier, used both in {@code app.notifications.channels} and in the {@code notifications.channel} column. */
    String id();

    /**
     * Delivers {@code message}; must be idempotent per {@link NotificationMessage#dedupeKey()}.
     *
     * @return {@code true} if this call delivered something new, {@code false} if the occurrence had already been delivered
     */
    boolean deliver(NotificationMessage message);
}
