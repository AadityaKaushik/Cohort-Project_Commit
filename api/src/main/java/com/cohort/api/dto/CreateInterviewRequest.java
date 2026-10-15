package com.cohort.api.dto;

import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewRoundType;
import com.cohort.api.validation.CleanName;
import com.cohort.api.validation.InputText;
import com.cohort.api.validation.MeaningfulText;
import com.cohort.api.validation.PlausibleDate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateInterviewRequest(
        @NotNull UUID applicationId,
        @PlausibleDate Instant scheduledAt,
        @PlausibleDate Instant completedAt,
        @NotNull @Min(1) @Max(Short.MAX_VALUE) Short roundNumber,
        @NotNull InterviewRoundType roundType,
        @Min(1) @Max(600) Integer durationMinutes,
        Interview.Mode mode,
        @Size(max = 200) @CleanName String interviewerName,
        @Size(max = 200) @CleanName String interviewerRole,
        Interview.Outcome outcome,
        @Min(1) @Max(5) Short perceivedDifficulty,
        @Size(max = 10000) @MeaningfulText String notes) {
    public CreateInterviewRequest {
        interviewerName = InputText.collapse(interviewerName);
        interviewerRole = InputText.collapse(interviewerRole);
    }
}
