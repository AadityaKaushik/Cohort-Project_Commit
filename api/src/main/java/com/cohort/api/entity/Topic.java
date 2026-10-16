package com.cohort.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "topics")
public class Topic {
    public enum Category { dsa, system_design, databases, os_networking, oop, behavioural }

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_difficulty", nullable = false)
    private Difficulty defaultDifficulty;

    @Column(name = "taxonomy_version", nullable = false)
    private short taxonomyVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Topic() {
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

    public Category getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Difficulty getDefaultDifficulty() {
        return defaultDifficulty;
    }

    public short getTaxonomyVersion() {
        return taxonomyVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
