package com.cohort.api.dto;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Map;

/** US-46: rejection stage distribution, with {@code no_response} kept as its own bucket (DR-02). */
public record RejectionDistributionResponse(Map<ApplicationStatus, Long> byStage, long noResponseCount, int sampleSize, boolean sufficientData) {
}
