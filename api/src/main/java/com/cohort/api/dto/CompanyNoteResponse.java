package com.cohort.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CompanyNoteResponse(UUID id, String body, Instant createdAt, Instant updatedAt) {
}
