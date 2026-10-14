package com.cohort.api.event;

import com.cohort.api.entity.InterviewRoundType;
import java.time.Instant;
import java.util.UUID;

public record UpcomingInterviewEvent(
        UUID userId,
        UUID interviewId,
        UUID applicationId,
        String companyName,
        String jobTitle,
        short roundNumber,
        InterviewRoundType roundType,
        Instant scheduledAt) implements DomainEvent {
    @Override
    public String occurrenceKey() {
        return "upcoming_interview:" + interviewId + ":" + scheduledAt.toEpochMilli();
    }
}
