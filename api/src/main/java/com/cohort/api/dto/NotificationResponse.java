package com.cohort.api.dto;

import com.cohort.api.entity.Notification;
import java.time.Instant;
import java.util.UUID;

/** US-79: one notification in the bell/list. {@code related_entity_*} is what the UI turns into a link to the application or interview it's about. */
public record NotificationResponse(
        UUID id,
        Notification.Type type,
        String title,
        String body,
        Notification.RelatedEntityType relatedEntityType,
        UUID relatedEntityId,
        boolean isRead,
        Instant readAt,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getRelatedEntityType(),
                notification.getRelatedEntityId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
