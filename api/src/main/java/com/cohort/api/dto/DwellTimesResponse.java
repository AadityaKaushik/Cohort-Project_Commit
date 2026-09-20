package com.cohort.api.dto;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Map;

/** US-48 / §20.7: median + IQR dwell time per pipeline stage, from completed transitions only, with censored (still-in-progress) applications reported separately (never folded into the median). */
public record DwellTimesResponse(Map<ApplicationStatus, DwellView> byStage) {

    public record DwellView(double medianDays, double p25Days, double p75Days, long completedCount, long censoredCount, boolean sufficientData) {
    }
}
