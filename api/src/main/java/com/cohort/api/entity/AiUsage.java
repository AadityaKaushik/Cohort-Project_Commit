package com.cohort.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "ai_usage_log")
public class AiUsage {
    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String feature;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private boolean succeeded;

    @Column(name = "prompt_chars", nullable = false)
    private int promptChars;

    @Column(name = "response_chars", nullable = false)
    private int responseChars;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiUsage() {
    }

    public AiUsage(UUID userId, String feature, String provider, boolean succeeded, int promptChars, int responseChars, int durationMs) {
        this.userId = userId;
        this.feature = feature;
        this.provider = provider;
        this.succeeded = succeeded;
        this.promptChars = promptChars;
        this.responseChars = responseChars;
        this.durationMs = durationMs;
    }

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }

    public String getFeature() { return feature; }

    public String getProvider() { return provider; }

    public boolean isSucceeded() { return succeeded; }

    public int getPromptChars() { return promptChars; }

    public int getResponseChars() { return responseChars; }

    public int getDurationMs() { return durationMs; }

    public Instant getCreatedAt() { return createdAt; }
}
