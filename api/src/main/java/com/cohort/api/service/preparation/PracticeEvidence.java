package com.cohort.api.service.preparation;

import java.time.Instant;

/** One practice attempt's contribution to the mastery formula (PROJECT_PLAN.md §21.4): a confidence rating (1-5) and when it happened. */
public record PracticeEvidence(short confidence, Instant occurredAt) {
}
