package com.cohort.api.service.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StallDetectionServiceTest {
    @Test
    void usesTheSystemDefaultWhenThePersonalMedianHasFewerThanFiveObservations() {
        // With 3 observations the personal median is ignored and the default of 10 days applies.
        StallDetectionService.StallEvaluation evaluation =
                StallDetectionService.evaluate(20.0, 2.0, 3, 10.0);

        assertThat(evaluation.basis()).isEqualTo(StallDetectionService.BASIS_SYSTEM_DEFAULT);
        assertThat(evaluation.thresholdDays()).isEqualTo(10.0);
        assertThat(evaluation.stalled()).isTrue();
    }

    @Test
    void usesThePersonalMedianTimesOnePointFiveOnceThereAreAtLeastFiveObservations() {
        StallDetectionService.StallEvaluation evaluation =
                StallDetectionService.evaluate(11.0, 8.0, 5, 10.0);

        assertThat(evaluation.basis()).isEqualTo(StallDetectionService.BASIS_PERSONAL);
        assertThat(evaluation.thresholdDays()).isEqualTo(12.0);
        assertThat(evaluation.stalled()).isFalse();
    }

    @Test
    void theSystemDefaultStillWinsWhenItExceedsThePersonalMedianTimesOnePointFive() {
        StallDetectionService.StallEvaluation evaluation =
                StallDetectionService.evaluate(5.0, 2.0, 7, 10.0);

        assertThat(evaluation.basis()).isEqualTo(StallDetectionService.BASIS_PERSONAL);
        assertThat(evaluation.thresholdDays()).isEqualTo(10.0);
        assertThat(evaluation.stalled()).isFalse();
    }

    @Test
    void flagsExactlyAtTheBoundaryAsNotYetStalled() {
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
