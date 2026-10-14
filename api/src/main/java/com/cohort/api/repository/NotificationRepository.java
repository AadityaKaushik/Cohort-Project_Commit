package com.cohort.api.repository;

import com.cohort.api.entity.Notification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends UserScopedRepository<Notification> {
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

    Page<Notification> findAllByUserIdAndIsRead(UUID userId, boolean isRead, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true, n.readAt = :now where n.userId = :userId and n.isRead = false")
    int markAllReadForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
