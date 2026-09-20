package com.cohort.api.service.preparation;

/**
 * {@code rawScore} is the blended, un-decayed [0,1] evidence score;
 * {@code masteryScore} is {@code rawScore} after time decay (§21.4). Both
 * are reported even when {@code sufficientEvidence} is false — the UI
 * decides whether to display a number, per the caller's own honesty
 * requirements (FR-67-style gating lives in {@code AnalyticsMetric}, not
 * here; this is a persisted-value engine, not an analytics metric).
 */
public record MasteryResult(double rawScore, double masteryScore, int observationCount, boolean sufficientEvidence) {
}
