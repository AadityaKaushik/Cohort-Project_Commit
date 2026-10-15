package com.cohort.api.service.preparation.review;

import java.time.Instant;
import java.time.LocalDate;

public record ReviewSchedule(Instant lastPractisedAt, LocalDate dueDate, boolean lastAttemptMissed) {
}
