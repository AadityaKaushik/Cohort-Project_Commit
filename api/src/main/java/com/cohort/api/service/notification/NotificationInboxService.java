package com.cohort.api.service.notification;

import com.cohort.api.dto.MarkAllReadResponse;
import com.cohort.api.dto.NotificationResponse;
import com.cohort.api.dto.PagedResponse;
import com.cohort.api.dto.UnreadCountResponse;
import com.cohort.api.entity.Notification;
import com.cohort.api.exception.NotificationNotFoundException;
import com.cohort.api.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-79 / FR-101: the read side of notifications -- the bell's list, its
 * unread count, and marking one or all as read. Deliberately separate from
 * {@link NotificationService}, which is the Strategy context for
 * <em>delivering</em> notifications; this class is plain, user-scoped CRUD
 * (ARCHITECTURE_REVISION_JAVA.md §4.2) and knows nothing about channels.
 */
@Service
public class NotificationInboxService {

    public enum Filter { all, unread, read }

    static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;

    public NotificationInboxService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** {@code all} lists unread first, newest first within each group -- the V20 index's exact order. */
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> list(UUID userId, Filter filter, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        Page<Notification> result = switch (filter) {
            case unread -> notificationRepository.findAllByUserIdAndIsRead(userId, false, newestFirst(safePage, safeSize));
            case read -> notificationRepository.findAllByUserIdAndIsRead(userId, true, newestFirst(safePage, safeSize));
            case all -> notificationRepository.findAllByUserId(userId, PageRequest.of(
                    safePage, safeSize, Sort.by(Sort.Order.asc("isRead"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        };
        List<NotificationResponse> items = result.getContent().stream().map(NotificationResponse::from).toList();
        return new PagedResponse<>(items, result.getTotalElements(), safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        return new UnreadCountResponse(notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(NotificationNotFoundException::new);
        notification.markRead(Instant.now());
        return NotificationResponse.from(notification);
    }

    @Transactional
    public MarkAllReadResponse markAllRead(UUID userId) {
        return new MarkAllReadResponse(notificationRepository.markAllReadForUser(userId, Instant.now()));
    }

    private static Pageable newestFirst(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }
}
