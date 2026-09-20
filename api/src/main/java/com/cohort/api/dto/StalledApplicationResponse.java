package com.cohort.api.dto;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.application.StallDetectionService;
import java.util.UUID;

/** US-18: a stalled application, with the threshold and basis that flagged it so "the flag explains its own threshold and sample size on hover." */
public record StalledApplicationResponse(
        UUID applicationId,
        String companyName,
        String jobTitle,
        ApplicationStatus currentStage,
        double daysInStage,
        double thresholdDays,
        String basis,
        int sampleSize) {

    public static StalledApplicationResponse from(StallDetectionService.StalledApplication stalled) {
        return new StalledApplicationResponse(
                stalled.applicationId(),
                stalled.companyName(),
                stalled.jobTitle(),
                stalled.currentStage(),
                stalled.evaluation().daysInStage(),
                stalled.evaluation().thresholdDays(),
                stalled.evaluation().basis(),
                stalled.evaluation().personalSampleSize());
    }
}
