package com.cohort.api.event;

import com.cohort.api.entity.InterviewRoundType;
import java.time.Instant;
import java.util.UUID;

/**
 * FR-99 / US-77: an interview is scheduled within the next 24 hours.
 * {@code scheduledAt} is part of the occurrence key, so rescheduling an
 * interview produces a fresh reminder for the new time rather than being
 * suppressed as "already reminded."
 */
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
