package com.cohort.api.service.notification;

import com.cohort.api.entity.Notification;
import java.util.Objects;
import java.util.UUID;

/**
 * The channel-agnostic content of one notification: what to tell which
 * user, and what it points at. {@link NotificationListener} builds these
 * from domain events; every {@link NotificationChannel} strategy receives
 * the exact same message and decides only <em>how</em> to deliver it.
 * {@code dedupeKey} is the originating event's occurrence key, so every
 * channel can honour "at most once per occurrence" in its own way.
 */
public record NotificationMessage(
        UUID userId,
        Notification.Type type,
        String title,
        String body,
        Notification.RelatedEntityType relatedEntityType,
        UUID relatedEntityId,
        String dedupeKey) {

    public NotificationMessage {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        if ((relatedEntityType == null) != (relatedEntityId == null)) {
            throw new IllegalArgumentException("relatedEntityType and relatedEntityId must be both set or both null");
        }
    }
}
