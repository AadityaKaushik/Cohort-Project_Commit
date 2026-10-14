package com.cohort.api.service.notification;

import com.cohort.api.repository.NotificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InAppNotificationChannel implements NotificationChannel {
    public static final String ID = "in_app";

    private final NotificationRepository notificationRepository;

    public InAppNotificationChannel(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean deliver(NotificationMessage message) {
        int inserted = notificationRepository.insertIfAbsent(
                message.userId(),
                message.type().name(),
                message.title(),
                message.body(),
                message.relatedEntityType() == null ? null : message.relatedEntityType().name(),
                message.relatedEntityId() == null ? null : message.relatedEntityId().toString(),
                ID,
                message.dedupeKey());
        return inserted > 0;
    }
}
