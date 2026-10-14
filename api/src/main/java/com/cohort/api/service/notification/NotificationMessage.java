package com.cohort.api.service.notification;

import com.cohort.api.entity.Notification;
import java.util.Objects;
import java.util.UUID;

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
