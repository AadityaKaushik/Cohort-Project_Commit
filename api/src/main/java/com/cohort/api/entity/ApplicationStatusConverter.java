package com.cohort.api.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link ApplicationStatus} to the lowercase snake_case text values the
 * `applications.status` / `applications.current_stage` / `application_status_history`
 * CHECK constraints expect (e.g. {@code FINAL_ROUND} <-> {@code "final_round"}).
 * {@code autoApply = true} so every entity field of this type converts
 * automatically without an explicit {@code @Convert} on each one.
 */
@Converter(autoApply = true)
public class ApplicationStatusConverter implements AttributeConverter<ApplicationStatus, String> {

    @Override
    public String convertToDatabaseColumn(ApplicationStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public ApplicationStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ApplicationStatus.valueOf(dbData.toUpperCase());
    }
}
