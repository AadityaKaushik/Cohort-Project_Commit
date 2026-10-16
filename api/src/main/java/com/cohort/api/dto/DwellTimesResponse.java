package com.cohort.api.dto;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Map;

/** US-48: median and IQR dwell time per stage from completed transitions. In-progress applications are reported separately. */
public record DwellTimesResponse(Map<ApplicationStatus, DwellView> byStage) {

    public record DwellView(double medianDays, double p25Days, double p75Days, long completedCount, long censoredCount, boolean sufficientData) {
    }
}
