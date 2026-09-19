package com.cohort.api.dto;

import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewRoundType;
import java.time.Instant;
import java.util.UUID;

/**
 * The small interview slice embedded in {@link ApplicationDetailResponse}
 * (FR-17: "view an application's full detail, including related
 * interviews"). Full interview detail — questions, feedback, interviewer
 * fields — lives at {@code GET /interviews/{id}}; this is just enough to
 * render a row inside the application page's own interview list.
 */
public record ApplicationInterviewSummary(
        UUID id,
        short roundNumber,
        InterviewRoundType roundType,
        Instant scheduledAt,
        Instant completedAt,
        Interview.Outcome outcome) {

    public static ApplicationInterviewSummary from(Interview interview) {
        return new ApplicationInterviewSummary(
                interview.getId(),
                interview.getRoundNumber(),
                interview.getRoundType(),
                interview.getScheduledAt(),
                interview.getCompletedAt(),
                interview.getOutcome());
    }
}
