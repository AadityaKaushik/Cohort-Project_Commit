package com.cohort.api.dto;

import com.cohort.api.entity.ResumeSection;
import java.time.LocalDate;
import java.util.UUID;

public record ResumeSectionResponse(
        UUID id,
        ResumeSection.SectionType sectionType,
        String title,
        String organisation,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        int sortOrder) {
}
