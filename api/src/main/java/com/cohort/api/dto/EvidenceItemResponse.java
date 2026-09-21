package com.cohort.api.dto;

import java.time.Instant;

/** Mirrors {@code recommendation_evidence} rows / {@code EvidenceItem} (US-51). */
public record EvidenceItemResponse(String label, String detail, Instant occurredAt) {
}
