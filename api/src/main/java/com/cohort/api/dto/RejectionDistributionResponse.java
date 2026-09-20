package com.cohort.api.dto;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Map;

/** US-46 / §20.7: rejection stage distribution, plus the distinct {@code no_response} bucket (DR-02: "never merged into rejections"). */
public record RejectionDistributionResponse(Map<ApplicationStatus, Long> byStage, long noResponseCount, int sampleSize, boolean sufficientData) {
}
