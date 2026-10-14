package com.cohort.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "interview_feedback")
public class InterviewFeedback {
    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "interview_id", nullable = false, unique = true)
    private UUID interviewId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "self_rating")
    private Short selfRating;

    @Column(name = "what_went_well")
    private String whatWentWell;

    @Column(name = "what_went_poorly")
    private String whatWentPoorly;

    @Column(name = "improvement_areas")
    private String improvementAreas;

    @Column(name = "external_feedback")
    private String externalFeedback;

    @Column(name = "is_draft", nullable = false)
    private boolean isDraft = true;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InterviewFeedback() {
        // JPA
    }

    public InterviewFeedback(UUID interviewId, UUID userId) {
        this.interviewId = interviewId;
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInterviewId() {
        return interviewId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Short getSelfRating() {
        return selfRating;
    }

    public void setSelfRating(Short selfRating) {
        this.selfRating = selfRating;
    }

    public String getWhatWentWell() {
        return whatWentWell;
    }

    public void setWhatWentWell(String whatWentWell) {
        this.whatWentWell = whatWentWell;
    }

    public String getWhatWentPoorly() {
        return whatWentPoorly;
    }

    public void setWhatWentPoorly(String whatWentPoorly) {
        this.whatWentPoorly = whatWentPoorly;
    }

    public String getImprovementAreas() {
        return improvementAreas;
    }

    public void setImprovementAreas(String improvementAreas) {
        this.improvementAreas = improvementAreas;
    }

    public String getExternalFeedback() {
        return externalFeedback;
    }

    public void setExternalFeedback(String externalFeedback) {
        this.externalFeedback = externalFeedback;
    }

    public boolean isDraft() {
        return isDraft;
    }

    public void setDraft(boolean draft) {
        isDraft = draft;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
