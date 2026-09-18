package com.cohort.api.dto;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationStatusHistory;
import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusHistoryResponse(
        UUID id, ApplicationStatus fromStatus, ApplicationStatus toStatus, String note, Instant changedAt) {

    public static ApplicationStatusHistoryResponse from(ApplicationStatusHistory history) {
        return new ApplicationStatusHistoryResponse(
                history.getId(), history.getFromStatus(), history.getToStatus(), history.getNote(), history.getChangedAt());
    }
}
