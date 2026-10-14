package com.cohort.api.service.preparation;

import java.time.Instant;

public record PracticeEvidence(short confidence, Instant occurredAt) {
}
