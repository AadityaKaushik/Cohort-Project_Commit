package com.cohort.api.repository;

import com.cohort.api.entity.Notification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Strictly user-owned (FR-100, FR-95): every finder is scoped by
 * {@code userId} via {@link UserScopedRepository}. {@link #insertIfAbsent}
 * is the one write path, and it too takes {@code userId} explicitly -- the
 * partial unique index on {@code (user_id, dedupe_key)} (V20) turns a
 * repeat of the same occurrence into a no-op rather than a duplicate row,
 * which is what lets detection run on a schedule without re-notifying.
 */
public interface NotificationRepository extends UserScopedRepository<Notification> {

    /**
     * {@code relatedEntityId} is bound as text and cast in SQL: a null
     * {@code UUID} parameter in a native query has no type Postgres can
     * infer, whereas a null text value casts cleanly to a null uuid.
     *
     * @return 1 if a new row was written, 0 if this occurrence had already been notified
     */
    @Modifying
    @Query(value = """
            INSERT INTO notifications (user_id, type, title, body, related_entity_type, related_entity_id, channel, dedupe_key)
            VALUES (:userId, :type, :title, :body, :relatedEntityType, CAST(:relatedEntityId AS uuid), :channel, :dedupeKey)
            ON CONFLICT (user_id, dedupe_key) WHERE dedupe_key IS NOT NULL DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("userId") UUID userId,
            @Param("type") String type,
            @Param("title") String title,
            @Param("body") String body,
            @Param("relatedEntityType") String relatedEntityType,
            @Param("relatedEntityId") String relatedEntityId,
            @Param("channel") String channel,
            @Param("dedupeKey") String dedupeKey);

    /** FR-101: the unread and read views, each on its own. */
    Page<Notification> findAllByUserIdAndIsRead(UUID userId, boolean isRead, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);

    /** US-79 "mark all as read": one bulk update, scoped to the caller; returns how many rows changed. */
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true, n.readAt = :now where n.userId = :userId and n.isRead = false")
    int markAllReadForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
