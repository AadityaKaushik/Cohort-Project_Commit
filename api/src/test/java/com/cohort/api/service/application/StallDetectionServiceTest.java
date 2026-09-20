package com.cohort.api.service.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * P36's mandatory verify: unit test on the pure threshold logic behind
 * US-18's stall flag, independent of any repository -- exactly the formula
 * from PROJECT_PLAN.md §20.7: {@code days_in_stage > max(personal_median *
 * 1.5, system_default)}, with the personal median only trusted once it has
 * at least 5 observations (the same threshold {@link
 * com.cohort.api.service.preparation.analytics.AnalyticsMetric#DEFAULT_MINIMUM_SAMPLE_SIZE}
 * enforces everywhere else in this codebase).
 */
class StallDetectionServiceTest {

    @Test
    void usesTheSystemDefaultWhenThePersonalMedianHasFewerThanFiveObservations() {
        // personal median (2 days) would normally make a 20-day wait look wildly stalled,
        // but with only 3 observations it must not be trusted -- the system default (10) applies instead.
        StallDetectionService.StallEvaluation evaluation =
                StallDetectionService.evaluate(20.0, 2.0, 3, 10.0);

        assertThat(evaluation.basis()).isEqualTo(StallDetectionService.BASIS_SYSTEM_DEFAULT);
        assertThat(evaluation.thresholdDays()).isEqualTo(10.0);
        assertThat(evaluation.stalled()).isTrue();
    }

    @Test
    void usesThePersonalMedianTimesOnePointFiveOnceThereAreAtLeastFiveObservations() {
        // personal median 8 days * 1.5 = 12, which beats the system default of 10 -- the max() applies.
        StallDetectionService.StallEvaluation evaluation =
                StallDetectionService.evaluate(11.0, 8.0, 5, 10.0);

        assertThat(evaluation.basis()).isEqualTo(StallDetectionService.BASIS_PERSONAL);
        assertThat(evaluation.thresholdDays()).isEqualTo(12.0);
        assertThat(evaluation.stalled()).isFalse(); // 11 days has not yet exceeded the 12-day personal threshold
    }

    @Test
    void theSystemDefaultStillWinsWhenItExceedsThePersonalMedianTimesOnePointFive() {
        // personal median 2 days * 1.5 = 3, but the system default (10) is larger -- max() picks 10.
        StallDetectionService.StallEvaluation evaluation =
                StallDetectionService.evaluate(5.0, 2.0, 7, 10.0);

        assertThat(evaluation.basis()).isEqualTo(StallDetectionService.BASIS_PERSONAL);
        assertThat(evaluation.thresholdDays()).isEqualTo(10.0);
        assertThat(evaluation.stalled()).isFalse();
    }

    @Test
    void flagsExactlyAtTheBoundaryAsNotYetStalled() {
        // days_in_stage > threshold is strict -- exactly at the threshold is not stalled yet.
        StallDetectionService.StallEvaluation evaluation =
                StallDetectionService.evaluate(10.0, 0.0, 0, 10.0);

        assertThat(evaluation.stalled()).isFalse();
    }

    @Test
    void oneDayPastTheBoundaryIsStalled() {
        StallDetectionService.StallEvaluation evaluation =
                StallDetectionService.evaluate(10.0001, 0.0, 0, 10.0);

        assertThat(evaluation.stalled()).isTrue();
    }

    @Test
    void reportsThePersonalSampleSizeRegardlessOfWhichBasisWasUsed() {
        StallDetectionService.StallEvaluation insufficient = StallDetectionService.evaluate(1.0, 1.0, 4, 10.0);
        StallDetectionService.StallEvaluation sufficient = StallDetectionService.evaluate(1.0, 1.0, 5, 10.0);

        assertThat(insufficient.personalSampleSize()).isEqualTo(4);
        assertThat(sufficient.personalSampleSize()).isEqualTo(5);
    }
}
