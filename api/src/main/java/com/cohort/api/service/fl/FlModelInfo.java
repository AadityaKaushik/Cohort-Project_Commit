package com.cohort.api.service.fl;

import java.time.Instant;
import java.util.List;

public record FlModelInfo(int version, int roundNumber, int participantCount, Instant createdAt, List<Double> parameters) {
}
