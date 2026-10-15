package com.cohort.api.dto;

import java.time.LocalDate;
import java.util.List;

public record DirectoryCompanyListItem(
        String slug,
        String name,
        String industry,
        String website,
        List<String> roleFamilies,
        Integer difficultyLevel,
        String difficultyBasis,
        LocalDate lastChecked) {
}
