package com.cohort.api.entity;

import com.cohort.api.service.recommendation.RecommendationStatus;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "recommendations")
public class Recommendation {
    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_tier", nullable = false, length = 1)
    private EvidenceTier evidenceTier;

    @Column(name = "target_topic_id")
    private UUID targetTopicId;

    @Column(name = "target_application_id")
    private UUID targetApplicationId;

    @Column(nullable = false)
    private double score;

    @Column(nullable = false)
    private String headline;

    @Column(nullable = false)
    private String explanation;

    @Column(name = "observation_count", nullable = false)
    private int observationCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationStatus status = RecommendationStatus.active;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    @Column(name = "dismissed_reason")
    private String dismissedReason;

    @Column(name = "accepted_task_id")
    private UUID acceptedTaskId;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Recommendation() {
        // JPA
    }

    public Recommendation(
            UUID userId, RecommendationType type, EvidenceTier evidenceTier, UUID targetTopicId, UUID targetApplicationId,
            double score, String headline, String explanation, int observationCount, Instant generatedAt) {
        this.userId = userId;
        this.type = type;
        this.evidenceTier = evidenceTier;
        this.targetTopicId = targetTopicId;
        this.targetApplicationId = targetApplicationId;
        this.score = score;
        this.headline = headline;
        this.explanation = explanation;
        this.observationCount = observationCount;
        this.generatedAt = generatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public RecommendationType getType() {
        return type;
    }

    public EvidenceTier getEvidenceTier() {
        return evidenceTier;
    }

    public UUID getTargetTopicId() {
        return targetTopicId;
    }

    public UUID getTargetApplicationId() {
        return targetApplicationId;
    }

    public double getScore() {
        return score;
    }

    public String getHeadline() {
        return headline;
    }

    public String getExplanation() {
        return explanation;
    }

    public int getObservationCount() {
        return observationCount;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }

    public Instant getSnoozedUntil() {
        return snoozedUntil;
    }

    public void setSnoozedUntil(Instant snoozedUntil) {
        this.snoozedUntil = snoozedUntil;
    }

    public String getDismissedReason() {
        return dismissedReason;
    }

    public void setDismissedReason(String dismissedReason) {
        this.dismissedReason = dismissedReason;
    }

    public UUID getAcceptedTaskId() {
        return acceptedTaskId;
    }

    public void setAcceptedTaskId(UUID acceptedTaskId) {
        this.acceptedTaskId = acceptedTaskId;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
