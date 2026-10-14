package com.cohort.api.service.recommendation;

import java.time.Instant;

public record EvidenceItem(String label, String detail, Instant occurredAt) {
}
