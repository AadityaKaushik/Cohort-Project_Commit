package com.cohort.api.dto;

import com.cohort.api.entity.Notification;
import java.time.Instant;
import java.util.UUID;

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
