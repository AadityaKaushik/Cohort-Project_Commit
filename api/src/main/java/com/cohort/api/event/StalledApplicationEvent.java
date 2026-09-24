package com.cohort.api.event;

import com.cohort.api.entity.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * FR-99 / US-76: an application has sat in its current stage longer than
 * the US-18 stall threshold ({@code StallDetectionService}'s personal-median
 * or system-default rule). {@code stageEnteredAt} is part of the occurrence
 * key, so one stay in one stage is reported once; moving the application on
 * (and stalling again later) is a genuinely new occurrence.
 */
public record StalledApplicationEvent(
        UUID userId,
        UUID applicationId,
        String companyName,
        String jobTitle,
        ApplicationStatus stage,
        Instant stageEnteredAt,
        double daysInStage,
        double thresholdDays) implements DomainEvent {

    @Override
    public String occurrenceKey() {
        return "stalled_application:" + applicationId + ":" + stage + ":" + stageEnteredAt.toEpochMilli();
    }
}
