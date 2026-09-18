package com.cohort.api.dto;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import java.time.LocalDate;

/**
 * Every combinable filter accepted by {@code GET /applications} (US-16, US-17,
 * FR-15, FR-16). All fields are optional; {@code null}/blank means "no
 * restriction on this field" once handed to
 * {@link com.cohort.api.repository.specification.ApplicationSpecifications#combine}.
 */
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
