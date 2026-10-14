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
@Table(name = "fl_rounds", schema = "fl")
public class FlRound {
    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(nullable = false)
    private boolean accepted;

    @Column
    private String reason;

    @Column(name = "cohort_size", nullable = false)
    private int cohortSize;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FlRound() {
        // JPA
    }

    public FlRound(int roundNumber, boolean accepted, String reason, int cohortSize) {
        this.roundNumber = roundNumber;
        this.accepted = accepted;
        this.reason = reason;
        this.cohortSize = cohortSize;
    }

    public UUID getId() {
        return id;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getReason() {
        return reason;
    }

    public int getCohortSize() {
        return cohortSize;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
