package com.cohort.api.dto;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import java.time.LocalDate;

public record ApplicationSearchCriteria(
        ApplicationStatus status,
        ApplicationStatus stage,
        Application.Priority priority,
        Application.WorkMode workMode,
        String tag,
        LocalDate dateFrom,
        LocalDate dateTo,
        String q) {
    public static final ApplicationSearchCriteria EMPTY =
            new ApplicationSearchCriteria(null, null, null, null, null, null, null, null);
}
