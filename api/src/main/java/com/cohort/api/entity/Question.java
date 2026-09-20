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

/**
 * One entry in the question bank (US-35/36, PROJECT_PLAN.md §17.2 line
 * 1543-ish). A row is either system-owned ({@code isSystem = true},
 * {@code userId = null} — seeded, read-only) or user-owned ({@code
 * isSystem = false}, {@code userId} set — added directly or forked from a
 * system question via {@code sourceQuestionId}); the V14 migration's
 * {@code chk_questions_ownership} CHECK constraint makes the two cases the
 * only ones that can exist. Every question links to exactly one topic
 * (US-38 acceptance criterion, {@code topicId} not-null at the DB level).
 */
@Entity
@Table(name = "questions")
public class Question {

    public enum Category { dsa, technical, behavioural, hr, system_design, company_specific }

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column
    private Difficulty difficulty;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "source_question_id")
    private UUID sourceQuestionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Question() {
        // JPA
    }

    /** Factory for a user's own question (US-36) — {@code isSystem} is always false through this path. */
    public static Question ownedByUser(UUID userId, UUID topicId, String questionText, Category category) {
        Question question = new Question();
        question.userId = userId;
        question.topicId = topicId;
        question.questionText = questionText;
        question.category = category;
        question.isSystem = false;
        return question;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
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

    public boolean isSystem() {
        return isSystem;
    }

    public UUID getSourceQuestionId() {
        return sourceQuestionId;
    }

    public void setSourceQuestionId(UUID sourceQuestionId) {
        this.sourceQuestionId = sourceQuestionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
