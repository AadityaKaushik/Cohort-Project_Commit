package com.cohort.api.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

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
