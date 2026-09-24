package com.cohort.api.event;

import java.util.UUID;

/**
 * Observer pattern (ARCHITECTURE_REVISION_JAVA.md §4.1, FR-99): the common
 * shape of every domain event a detector publishes through Spring's
 * {@code ApplicationEventPublisher}. Each event is an immutable fact about
 * one user's data -- "this happened" -- never an instruction to any
 * particular subscriber, which is what lets detectors stay ignorant of who
 * (if anyone) is listening.
 *
 * <p>{@link #occurrenceKey()} names the specific occurrence the event
 * reports (one application's stay in one stage, one interview at one
 * scheduled time), so a subscriber that must act at most once per
 * occurrence -- such as the in-app notification channel -- can deduplicate
 * without the detector having to remember what it already published.
 */
public sealed interface DomainEvent permits StalledApplicationEvent, UpcomingInterviewEvent, MissingDebriefEvent {

    /** The one user this event is about -- every subscriber must scope its side effects to this user only (FR-100). */
    UUID userId();

    /** Stable identity of the occurrence being reported; equal keys mean "the same thing, seen again." */
    String occurrenceKey();
}
