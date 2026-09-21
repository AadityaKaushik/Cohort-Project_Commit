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
 * One observation behind a {@link Recommendation} (US-51, V18) --
 * {@link com.cohort.api.service.recommendation.EvidenceItem} persisted.
 * Owned indirectly through {@code recommendation_id}, not by a direct
 * {@code user_id} column (same rationale as {@code Topic} not extending
 * {@code UserScopedRepository}); access is always mediated by first loading
 * the owning {@link Recommendation} through {@code RecommendationRepository}.
 * Deleting a {@code Recommendation} row cascades to these at the database
 * level (V18's {@code ON DELETE CASCADE}), so nothing here needs an
 * explicit bulk-delete method for that case.
 */
@Entity
@Table(name = "recommendation_evidence")
public class RecommendationEvidence {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "recommendation_id", nullable = false)
    private UUID recommendationId;

    @Column(nullable = false)
    private String label;

    @Column
    private String detail;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RecommendationEvidence() {
        // JPA
    }

    public RecommendationEvidence(UUID recommendationId, String label, String detail, Instant occurredAt) {
        this.recommendationId = recommendationId;
        this.label = label;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRecommendationId() {
        return recommendationId;
    }

    public String getLabel() {
        return label;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
