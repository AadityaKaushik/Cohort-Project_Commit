package com.cohort.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CompanyListItem(
        UUID id,
        String name,
        String website,
        String industry,
        int applicationCount,
        int activeApplicationCount,
        int contactCount,
        Instant lastActivityAt) {
}
