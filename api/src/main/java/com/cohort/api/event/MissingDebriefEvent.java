package com.cohort.api.event;

import com.cohort.api.entity.InterviewRoundType;
import java.time.Instant;
import java.util.UUID;

public record MissingDebriefEvent(
        UUID userId,
        UUID interviewId,
        UUID applicationId,
        String companyName,
        String jobTitle,
        short roundNumber,
        InterviewRoundType roundType,
        Instant scheduledAt,
        boolean draftStarted) implements DomainEvent {
    @Override
    public String occurrenceKey() {
        return "missing_debrief:" + interviewId;
    }
}
