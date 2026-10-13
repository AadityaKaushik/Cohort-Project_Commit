package com.cohort.api.event;

import java.time.Instant;
import java.util.UUID;

/**
 * US-95: a contact the user has not touched for longer than the cold threshold.
 * {@code lastTouchedAt} is part of the occurrence key, so a contact that is
 * reached out to and later goes cold again is a fresh occurrence.
 */
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
