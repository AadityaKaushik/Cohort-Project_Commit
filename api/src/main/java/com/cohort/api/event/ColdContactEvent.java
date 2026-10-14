package com.cohort.api.event;

import java.time.Instant;
import java.util.UUID;

/** A contact not touched for longer than the cold threshold. */
public record ColdContactEvent(
        UUID userId,
        UUID contactId,
        String contactName,
        String companyName,
        boolean referralPending,
        Instant lastTouchedAt,
        long daysSinceContact) implements DomainEvent {

    @Override
    public String occurrenceKey() {
        return "cold_contact:" + contactId + ":" + lastTouchedAt.toEpochMilli();
    }
}
