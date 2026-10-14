package com.cohort.api.dto;

import com.cohort.api.entity.ApplicationStatus;
import java.time.LocalDate;
import java.util.List;

public record ApplicationImportResponse(
        boolean dryRun,
        int total,
        int importable,
        int duplicates,
        int invalid,
        int created,
        List<Column> columns,
        List<Row> rows) {

    public record Column(String header, String field) {
    }

    public record Row(
            int line,
            String company,
            String role,
            ApplicationStatus status,
            LocalDate dateApplied,
            String location,
            String url,
            String outcome,
            List<String> warnings) {
    }
}
