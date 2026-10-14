package com.cohort.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "privacy_ledger_entries", schema = "fl")
public class PrivacyLedgerEntry {
    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "round_id", nullable = false)
    private UUID roundId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "pre_clip_norm", nullable = false)
    private double preClipNorm;

    @Column(name = "post_clip_norm", nullable = false)
    private double postClipNorm;

    @Column(name = "noise_multiplier", nullable = false)
    private double noiseMultiplier;

    @Column(name = "cohort_size", nullable = false)
    private int cohortSize;

    @Column(nullable = false)
    private double epsilon;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transmitted_vector", nullable = false)
    private List<Double> transmittedVector;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PrivacyLedgerEntry() {
        // JPA
    }

    public PrivacyLedgerEntry(
            UUID roundId, UUID userId, int schemaVersion, double preClipNorm, double postClipNorm,
            double noiseMultiplier, int cohortSize, double epsilon, List<Double> transmittedVector) {
        this.roundId = roundId;
        this.userId = userId;
        this.schemaVersion = schemaVersion;
        this.preClipNorm = preClipNorm;
        this.postClipNorm = postClipNorm;
        this.noiseMultiplier = noiseMultiplier;
        this.cohortSize = cohortSize;
        this.epsilon = epsilon;
        this.transmittedVector = List.copyOf(transmittedVector);
    }

    public UUID getId() {
        return id;
    }

    public UUID getRoundId() {
        return roundId;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public double getPreClipNorm() {
        return preClipNorm;
    }

    public double getPostClipNorm() {
        return postClipNorm;
    }

    public double getNoiseMultiplier() {
        return noiseMultiplier;
    }

    public int getCohortSize() {
        return cohortSize;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public List<Double> getTransmittedVector() {
        return transmittedVector;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
