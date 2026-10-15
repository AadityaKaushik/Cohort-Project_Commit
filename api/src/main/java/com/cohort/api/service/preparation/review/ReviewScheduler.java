package com.cohort.api.service.preparation.review;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ReviewScheduler {
    static final List<Integer> INTERVAL_DAYS = List.of(1, 3, 7, 14, 30);

    public Optional<ReviewSchedule> schedule(List<ReviewAttempt> attempts) {
        if (attempts.isEmpty()) {
            return Optional.empty();
        }
        List<ReviewAttempt> ordered = attempts.stream()
                .sorted(Comparator.comparing(ReviewAttempt::attemptedAt))
                .toList();

        int streak = 0;
        for (ReviewAttempt attempt : ordered) {
            streak = attempt.missed() ? 0 : streak + 1;
        }

        ReviewAttempt last = ordered.get(ordered.size() - 1);
        int interval = INTERVAL_DAYS.get(Math.min(streak, INTERVAL_DAYS.size() - 1));
        Instant lastAt = last.attemptedAt();
        return Optional.of(new ReviewSchedule(
                lastAt, lastAt.atZone(ZoneOffset.UTC).toLocalDate().plusDays(interval), last.missed()));
    }
}
