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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "interview_questions")
public class InterviewQuestion {
    public enum Category { dsa, technical, behavioural, hr, system_design, company_specific }

    public enum Difficulty { easy, medium, hard }

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "interview_id", nullable = false)
    private UUID interviewId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Column(name = "topic_id")
    private UUID topicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column
    private Difficulty difficulty;

    @Column(name = "self_rating")
    private Short selfRating;

    @Column(name = "time_taken_minutes")
    private Integer timeTakenMinutes;

    @Column(name = "linked_question_id")
    private UUID linkedQuestionId;

    @Column
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InterviewQuestion() {
    }

    public InterviewQuestion(UUID interviewId, UUID userId, String questionText, Category category) {
        this.interviewId = interviewId;
        this.userId = userId;
        this.questionText = questionText;
        this.category = category;
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

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Short getSelfRating() {
        return selfRating;
    }

    public void setSelfRating(Short selfRating) {
        this.selfRating = selfRating;
    }

    public Integer getTimeTakenMinutes() {
        return timeTakenMinutes;
    }

    public void setTimeTakenMinutes(Integer timeTakenMinutes) {
        this.timeTakenMinutes = timeTakenMinutes;
    }

    public UUID getLinkedQuestionId() {
        return linkedQuestionId;
    }

    public void setLinkedQuestionId(UUID linkedQuestionId) {
        this.linkedQuestionId = linkedQuestionId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
