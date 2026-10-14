package com.cohort.api.dto;

import java.time.Instant;

public record EvidenceItemResponse(String label, String detail, Instant occurredAt) {
}
