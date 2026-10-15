package com.cohort.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One sourced interview pattern for a role family at a company. Read only. */
@Entity
@Table(name = "company_role_patterns")
public class CompanyRolePattern {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "role_family", nullable = false)
    private String roleFamily;

    @Column(nullable = false)
    private String level;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Map<String, Object>> rounds = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topic_slugs", nullable = false)
    private List<String> topicSlugs = List.of();

    private String notes;

    @Column(name = "experience_summary")
    private String experienceSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> pay;

    @Column(name = "difficulty_level")
    private Short difficultyLevel;

    @Column(name = "difficulty_basis")
    private String difficultyBasis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Map<String, Object>> sources = List.of();

    @Column(name = "last_checked", nullable = false)
    private LocalDate lastChecked;

    protected CompanyRolePattern() {
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getRoleFamily() {
        return roleFamily;
    }

    public String getLevel() {
        return level;
    }

    public List<Map<String, Object>> getRounds() {
        return rounds;
    }

    public List<String> getTopicSlugs() {
        return topicSlugs;
    }

    public String getNotes() {
        return notes;
    }

    public String getExperienceSummary() {
        return experienceSummary;
    }

    public Map<String, Object> getPay() {
        return pay;
    }

    public Short getDifficultyLevel() {
        return difficultyLevel;
    }

    public String getDifficultyBasis() {
        return difficultyBasis;
    }

    public List<Map<String, Object>> getSources() {
        return sources;
    }

    public LocalDate getLastChecked() {
        return lastChecked;
    }
}
