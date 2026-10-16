package com.cohort.api.dto;

import com.cohort.api.validation.MeaningfulText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateStoryRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) @MeaningfulText String situation,
        @Size(max = 5000) @MeaningfulText String task,
        @Size(max = 5000) @MeaningfulText String action,
        @Size(max = 5000) @MeaningfulText String result,
        List<@Pattern(regexp = UpdateStoryRequest.COMPETENCY_PATTERN, message = "Unknown competency.") String> competencies,
        UUID interviewId) {
}
