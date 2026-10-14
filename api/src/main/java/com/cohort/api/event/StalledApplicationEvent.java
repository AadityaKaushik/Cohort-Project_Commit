package com.cohort.api.event;

import com.cohort.api.entity.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;

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
