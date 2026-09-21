package com.cohort.api.service.recommendation;

/**
 * Mirrors the {@code recommendations.status} CHECK constraint (V17).
 * US-55: accept links a preparation task, snooze hides the row for 7 days,
 * dismiss records a reason and suppresses that recommendation type for 30
 * days -- {@code RecommendationFacade} (P42) is what enforces those
 * durations when it regenerates {@code active} rows.
 */
public enum RecommendationStatus {
    active,
    accepted,
    snoozed,
    dismissed
}
