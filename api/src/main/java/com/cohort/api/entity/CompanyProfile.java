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

/** Reference data for the company directory (US-96). Read only. */
@Entity
@Table(name = "company_profiles")
public class CompanyProfile {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    private String industry;

    private String locations;

    @Column(name = "size_band")
    private String sizeBand;

    private String website;

    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Map<String, Object>> sources = List.of();

    @Column(name = "last_checked", nullable = false)
    private LocalDate lastChecked;

    protected CompanyProfile() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getIndustry() {
        return industry;
    }

    public String getLocations() {
        return locations;
    }

    public String getSizeBand() {
        return sizeBand;
    }

    public String getWebsite() {
        return website;
    }

    public String getSummary() {
        return summary;
    }

    public List<Map<String, Object>> getSources() {
        return sources;
    }

    public LocalDate getLastChecked() {
        return lastChecked;
    }
}
