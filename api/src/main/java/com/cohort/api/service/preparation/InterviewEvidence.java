package com.cohort.api.service.preparation;

import java.time.Instant;

/** One interview question's contribution to the mastery formula (PROJECT_PLAN.md §21.4): a self-rating (1-5) and when it happened. */
public record InterviewEvidence(short selfRating, Instant occurredAt) {
}
