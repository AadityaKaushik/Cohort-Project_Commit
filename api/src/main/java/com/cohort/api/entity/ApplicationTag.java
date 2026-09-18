package com.cohort.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

/** The application &lt;-&gt; tag join row (V6 migration). Plain association row; no lifecycle of its own beyond insert/delete. */
@Entity
@Table(name = "application_tags")
@IdClass(ApplicationTagId.class)
public class ApplicationTag {

    @Id
    @Column(name = "application_id")
    private UUID applicationId;

    @Id
    @Column(name = "tag_id")
    private UUID tagId;

    protected ApplicationTag() {
        // JPA
    }

    public ApplicationTag(UUID applicationId, UUID tagId) {
        this.applicationId = applicationId;
        this.tagId = tagId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getTagId() {
        return tagId;
    }
}
