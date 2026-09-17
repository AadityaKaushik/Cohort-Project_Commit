package com.cohort.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "users")
public class User {

    public enum Role { user, admin }

    public enum FlPrivacyPosture { strict, balanced, contributor }

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "target_role")
    private String targetRole;

    @Column(name = "graduation_year")
    private Short graduationYear;

    @Column
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.user;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "fl_opt_in", nullable = false)
    private boolean flOptIn = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "fl_privacy_posture", nullable = false)
    private FlPrivacyPosture flPrivacyPosture = FlPrivacyPosture.balanced;

    @Column(name = "epsilon_spent", nullable = false)
    private BigDecimal epsilonSpent = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
        // JPA
    }

    public User(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public Short getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Short graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isFlOptIn() {
        return flOptIn;
    }

    public FlPrivacyPosture getFlPrivacyPosture() {
        return flPrivacyPosture;
    }

    public BigDecimal getEpsilonSpent() {
        return epsilonSpent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
