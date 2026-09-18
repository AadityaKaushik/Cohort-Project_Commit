package com.cohort.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * A user-scoped, free-form label (V6 migration, US-19). Full tag CRUD and
 * the bulk-tagging UI land later (US-19, Sprint 5) — this entity exists now
 * only so {@link com.cohort.api.repository.specification.ApplicationSpecifications#hasTag}
 * can filter applications by tag name (FR-16), which is in scope for this
 * phase even though managing tags isn't yet.
 */
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Tag() {
        // JPA
    }

    public Tag(UUID userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
