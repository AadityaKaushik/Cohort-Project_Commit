package com.cohort.api.dto;

import com.cohort.api.validation.MeaningfulText;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** PATCH semantics: only non-null fields are applied. */
public record UpdateStoryRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 5000) @MeaningfulText String situation,
        @Size(max = 5000) @MeaningfulText String task,
        @Size(max = 5000) @MeaningfulText String action,
        @Size(max = 5000) @MeaningfulText String result,
        List<@Pattern(regexp = COMPETENCY_PATTERN, message = "Unknown competency.") String> competencies,
        UUID interviewId) {

    static final String COMPETENCY_PATTERN =
            "leadership|teamwork|conflict|failure|ownership|deadline|learning|impact|communication|problem_solving";
}
