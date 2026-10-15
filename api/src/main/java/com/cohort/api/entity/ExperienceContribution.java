package com.cohort.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** US-96: one candidate's anonymous account. Holds a per-company hash, never the user id. */
@Entity
@Table(name = "experience_contributions")
public class ExperienceContribution {

    public enum Level { fresher, experienced }

    public enum Outcome { offer, rejected, withdrawn, pending }

    public enum RoundType {
        online_test, coding, technical, system_design, case_study, group_discussion, hr, managerial
    }

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "contributor_hash", nullable = false)
    private String contributorHash;

    @Column(name = "role_family", nullable = false)
    private String roleFamily;

    @Column(nullable = false)
    private String level;

    @Column(name = "contribution_year", nullable = false)
    private int year;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Map<String, Object>> rounds;

    @Column(name = "overall_difficulty", nullable = false)
    private int overallDifficulty;

    @Column(name = "overall_experience", nullable = false)
    private int overallExperience;

    @Column(nullable = false)
    private String outcome;

    private String tip;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExperienceContribution() {
    }

    public ExperienceContribution(
            UUID companyId, String contributorHash, String roleFamily, Level level, int year,
            List<Map<String, Object>> rounds, int overallDifficulty, int overallExperience, Outcome outcome,
            String tip) {
        this.companyId = companyId;
        this.contributorHash = contributorHash;
        this.roleFamily = roleFamily;
        this.level = level.name();
        this.year = year;
        this.rounds = rounds;
        this.overallDifficulty = overallDifficulty;
        this.overallExperience = overallExperience;
        this.outcome = outcome.name();
        this.tip = tip;
    }

    public UUID getId() { return id; }

    public UUID getCompanyId() { return companyId; }

    public String getContributorHash() { return contributorHash; }

    public String getRoleFamily() { return roleFamily; }

    public String getLevel() { return level; }

    public int getYear() { return year; }

    public List<Map<String, Object>> getRounds() { return rounds; }

    public int getOverallDifficulty() { return overallDifficulty; }

    public int getOverallExperience() { return overallExperience; }

    public String getOutcome() { return outcome; }

    public String getTip() { return tip; }

    public Instant getCreatedAt() { return createdAt; }
}
