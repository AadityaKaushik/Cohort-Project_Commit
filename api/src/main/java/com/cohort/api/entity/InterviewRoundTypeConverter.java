package com.cohort.api.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link InterviewRoundType} to the lowercase snake_case text the
 * {@code interviews.round_type} CHECK constraint expects (e.g. {@code
 * PHONE_SCREEN} <-> {@code "phone_screen"}), the same scheme as {@link
 * ApplicationStatusConverter}. {@code autoApply = true} so every entity
 * field of this type converts automatically.
 */
@Converter(autoApply = true)
public class InterviewRoundTypeConverter implements AttributeConverter<InterviewRoundType, String> {

    @Override
    public String convertToDatabaseColumn(InterviewRoundType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public InterviewRoundType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : InterviewRoundType.valueOf(dbData.toUpperCase());
    }
}
