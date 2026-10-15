package com.cohort.api.service.preparation.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewSchedulerTest {
    private static final Instant DAY_ONE = Instant.parse("2026-03-01T10:00:00Z");

    private final ReviewScheduler scheduler = new ReviewScheduler();

    private ReviewAttempt hit(int dayOffset) {
        return new ReviewAttempt(DAY_ONE.plusSeconds(dayOffset * 86_400L), false);
    }

    private ReviewAttempt miss(int dayOffset) {
        return new ReviewAttempt(DAY_ONE.plusSeconds(dayOffset * 86_400L), true);
    }

    private LocalDate dueFor(ReviewAttempt... attempts) {
        return scheduler.schedule(List.of(attempts)).orElseThrow().dueDate();
    }

    @Test
    void noAttemptsMeansNoSchedule() {
        assertThat(scheduler.schedule(List.of())).isEmpty();
    }

    @Test
    void firstSuccessIsDueInThreeDays() {
        assertThat(dueFor(hit(0))).isEqualTo(LocalDate.parse("2026-03-04"));
    }

    @Test
    void intervalsExpandWithEachSuccess() {
        assertThat(dueFor(hit(0), hit(3))).isEqualTo(LocalDate.parse("2026-03-11"));
        assertThat(dueFor(hit(0), hit(3), hit(10))).isEqualTo(LocalDate.parse("2026-03-25"));
        assertThat(dueFor(hit(0), hit(3), hit(10), hit(24))).isEqualTo(LocalDate.parse("2026-04-24"));
    }

    @Test
    void intervalStopsGrowingAtThirtyDays() {
        assertThat(dueFor(hit(0), hit(3), hit(10), hit(24), hit(54))).isEqualTo(LocalDate.parse("2026-05-24"));
    }

    @Test
    void aMissResetsToOneDay() {
        assertThat(dueFor(hit(0), hit(3), hit(10), miss(24))).isEqualTo(LocalDate.parse("2026-03-26"));
    }

    @Test
    void successAfterAMissStartsExpandingAgain() {
        assertThat(dueFor(hit(0), hit(3), miss(10), hit(11))).isEqualTo(LocalDate.parse("2026-03-15"));
    }

    @Test
    void attemptOrderDoesNotDependOnListOrder() {
        LocalDate due = scheduler.schedule(List.of(miss(10), hit(0), hit(3))).orElseThrow().dueDate();
        assertThat(due).isEqualTo(LocalDate.parse("2026-03-12"));
    }

    @Test
    void lastAttemptMissIsReported() {
        assertThat(scheduler.schedule(List.of(hit(0), miss(1))).orElseThrow().lastAttemptMissed()).isTrue();
    }
}
