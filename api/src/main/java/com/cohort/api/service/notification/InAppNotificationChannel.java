package com.cohort.api.service.notification;

import com.cohort.api.repository.NotificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The in-app {@link NotificationChannel} strategy (FR-100): delivery means
 * a row in {@code notifications}, which the bell (P61) reads. Deduplication
 * is delegated to the database -- {@code insertIfAbsent} is an
 * {@code ON CONFLICT DO NOTHING} against V20's unique
 * {@code (user_id, dedupe_key)} index -- so two detection runs racing each
 * other still produce one row, not two.
 *
 * <p>{@code REQUIRES_NEW}: events are published from inside detectors'
 * read-only transactions, and a notification write must neither be refused
 * by that read-only transaction nor be rolled back with it.
 */
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
