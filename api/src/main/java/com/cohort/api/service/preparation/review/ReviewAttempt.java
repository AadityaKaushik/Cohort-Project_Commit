package com.cohort.api.service.preparation.review;

import java.time.Instant;

public record ReviewAttempt(Instant attemptedAt, boolean missed) {
}
