package com.cohort.api.event;

import com.cohort.api.entity.InterviewRoundType;
import java.time.Instant;
import java.util.UUID;

/**
 * FR-99 / US-78: an interview's date has passed and no debrief has been
 * submitted for it. {@code draftStarted} distinguishes "nothing logged yet"
 * from "a draft exists but was never submitted," so a subscriber can word
 * its nudge accordingly; it is deliberately not part of the occurrence key
 * -- starting a draft does not make the missing debrief a new occurrence.
 */
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
