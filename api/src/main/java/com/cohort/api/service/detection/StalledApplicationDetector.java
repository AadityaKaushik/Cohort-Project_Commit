package com.cohort.api.service.detection;

import com.cohort.api.event.StalledApplicationEvent;
import com.cohort.api.service.application.StallDetectionService;
import com.cohort.api.service.application.StallDetectionService.StalledApplication;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class StalledApplicationDetector {
    private final StallDetectionService stallDetectionService;
    private final ApplicationEventPublisher eventPublisher;

    public StalledApplicationDetector(StallDetectionService stallDetectionService, ApplicationEventPublisher eventPublisher) {
        this.stallDetectionService = stallDetectionService;
        this.eventPublisher = eventPublisher;
    }

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
