package com.cohort.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "notifications")
public class Notification {
    public enum Type { stalled_application, upcoming_interview, missing_debrief, new_recommendation, fl_round_result }

    public enum RelatedEntityType { application, interview }

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_entity_type")
    private RelatedEntityType relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(nullable = false)
    private String channel;

    @Column(name = "dedupe_key")
    private String dedupeKey;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
        // JPA
    }

    public void markRead(Instant when) {
        if (!isRead) {
            isRead = true;
            readAt = when;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public RelatedEntityType getRelatedEntityType() {
        return relatedEntityType;
    }

    public UUID getRelatedEntityId() {
        return relatedEntityId;
    }

    public String getChannel() {
        return channel;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public boolean isRead() {
        return isRead;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
