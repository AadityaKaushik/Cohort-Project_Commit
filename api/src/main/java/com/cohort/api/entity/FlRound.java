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
 * Round-level metadata for one federated round (US-61/US-63, P55). Lives in
 * the {@code fl} schema, alongside {@link PrivacyLedgerEntry} — the same
 * isolated schema V19 (P50) created and scoped the {@code cohort_fl}
 * Postgres role to. Written by Java after {@code POST /fl/round}
 * (fl-service, P55) returns; never mutated afterwards — a round's outcome
 * (accepted/refused, cohort size, reason) is a historical fact.
 */
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
