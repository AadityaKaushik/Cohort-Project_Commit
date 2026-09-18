package com.cohort.api.dto;

import com.cohort.api.entity.Company;
import java.util.UUID;

public record CompanySummary(UUID id, String name, String website, String industry) {

    public static CompanySummary from(Company company) {
        return new CompanySummary(company.getId(), company.getName(), company.getWebsite(), company.getIndustry());
    }
}
