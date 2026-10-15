package com.cohort.api.dto;

import com.cohort.api.entity.ResumeSection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ResumeSectionRequest(
        @NotNull ResumeSection.SectionType sectionType,
        @Size(max = 200) String title,
        @Size(max = 200) String organisation,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 4000) String description) {
}
