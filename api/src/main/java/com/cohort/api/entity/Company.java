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

/**
 * User-scoped company record (ADR-004): every user maintains their own
 * companies rather than sharing one global table, so nothing about who is
 * applying where ever leaks across users via a shared row.
 */
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @Column
    private String website;

    @Column
    private String industry;

    // Named `userId` (not `createdByUserId`) purely so CompanyRepository can
    // extend the shared UserScopedRepository<T> base, which requires that
    // exact property name. The DB column is created_by_user_id (§17.2) —
    // "creator" and "owner" are the same person for a user-scoped record.
    @Column(name = "created_by_user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Company() {
        // JPA
    }

    public Company(String name, String normalizedName, UUID userId) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
