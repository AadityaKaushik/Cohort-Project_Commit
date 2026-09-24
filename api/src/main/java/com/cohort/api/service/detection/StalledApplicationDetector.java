package com.cohort.api.service.detection;

import com.cohort.api.event.StalledApplicationEvent;
import com.cohort.api.service.application.StallDetectionService;
import com.cohort.api.service.application.StallDetectionService.StalledApplication;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Observer pattern, subject side (ARCHITECTURE_REVISION_JAVA.md §4.1/§4.3,
 * FR-99, US-76): turns US-18's stall flag into a published
 * {@link StalledApplicationEvent}. It reuses {@link StallDetectionService}
 * for the decision itself -- the same personal-median-or-system-default
 * rule the dashboard and the Recommendations page already use -- rather than
 * re-deriving "stalled" a second way.
 *
 * <p>Its only outward dependency is Spring's generic
 * {@link ApplicationEventPublisher}: it has no reference to notifications,
 * channels, or any other subscriber, and publishing with zero listeners is a
 * perfectly valid outcome. Adding an email digest or an analytics-cache
 * invalidator later means adding a listener, never editing this class.
 */
@Component
public class StalledApplicationDetector {

    private final StallDetectionService stallDetectionService;
    private final ApplicationEventPublisher eventPublisher;

    public StalledApplicationDetector(StallDetectionService stallDetectionService, ApplicationEventPublisher eventPublisher) {
        this.stallDetectionService = stallDetectionService;
        this.eventPublisher = eventPublisher;
    }

    /** Publishes one event per currently-stalled application of {@code userId}; returns how many were published. */
    public int detect(UUID userId) {
        List<StalledApplication> stalled = stallDetectionService.detectStalled(userId);
        for (StalledApplication application : stalled) {
            eventPublisher.publishEvent(new StalledApplicationEvent(
                    userId,
                    application.applicationId(),
                    application.companyName(),
                    application.jobTitle(),
                    application.currentStage(),
                    application.stageEnteredAt(),
                    application.evaluation().daysInStage(),
                    application.evaluation().thresholdDays()));
        }
        return stalled.size();
    }
}
