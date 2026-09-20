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

/**
 * One logged practice session against a question (US-37, PROJECT_PLAN.md
 * §17.2 line 1545). Deliberately append-only — there is no update or
 * delete path in the service/controller layer, matching the schema
 * comment: "repeated attempts create rows, giving a confidence trajectory
 * rather than a single overwritten value," which is exactly the time
 * series {@link com.cohort.api.service.preparation.MasteryEngine} (P30)
 * applies exponential recency weighting over.
 */
@Entity
@Table(name = "practice_attempts")
public class PracticeAttempt {

    public enum Status { not_started, in_progress, confident, needs_review }

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column
    private Short confidence;

    @Column(name = "time_taken_minutes")
    private Integer timeTakenMinutes;

    @Column
    private String notes;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PracticeAttempt() {
        // JPA
    }

    public PracticeAttempt(UUID userId, UUID questionId, Status status, Instant attemptedAt) {
        this.userId = userId;
        this.questionId = questionId;
        this.status = status;
        this.attemptedAt = attemptedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public Status getStatus() {
        return status;
    }

    public Short getConfidence() {
        return confidence;
    }

    public void setConfidence(Short confidence) {
        this.confidence = confidence;
    }

    public Integer getTimeTakenMinutes() {
        return timeTakenMinutes;
    }

    public void setTimeTakenMinutes(Integer timeTakenMinutes) {
        this.timeTakenMinutes = timeTakenMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
