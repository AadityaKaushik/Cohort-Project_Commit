package com.cohort.api.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ApplicationTagId implements Serializable {
    private UUID applicationId;
    private UUID tagId;

    public ApplicationTagId() {
    }

    public ApplicationTagId(UUID applicationId, UUID tagId) {
        this.applicationId = applicationId;
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ApplicationTagId that)) {
            return false;
        }
        return Objects.equals(applicationId, that.applicationId) && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, tagId);
    }
}
