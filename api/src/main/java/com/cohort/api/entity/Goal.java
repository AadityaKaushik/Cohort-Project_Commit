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
@Table(name = "goals")
public class Goal {

    public enum Metric { applications, interviews, practice_attempts, contacts_reached }

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Metric metric;

    @Column(name = "weekly_target", nullable = false)
    private int weeklyTarget;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Goal() {
    }

    public Goal(UUID userId, Metric metric, int weeklyTarget) {
        this.userId = userId;
        this.metric = metric;
        this.weeklyTarget = weeklyTarget;
    }

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }

    public Metric getMetric() { return metric; }

    public int getWeeklyTarget() { return weeklyTarget; }

    public void setWeeklyTarget(int weeklyTarget) { this.weeklyTarget = weeklyTarget; }
}
