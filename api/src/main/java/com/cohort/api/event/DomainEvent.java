package com.cohort.api.event;

import java.util.UUID;

public sealed interface DomainEvent permits StalledApplicationEvent, UpcomingInterviewEvent, MissingDebriefEvent {
    // The one user this event is about -- every subscriber must scope its side effects to this user only (FR-100).
    UUID userId();

    String occurrenceKey();
}
