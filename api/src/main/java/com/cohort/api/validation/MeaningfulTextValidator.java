package com.cohort.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MeaningfulTextValidator implements ConstraintValidator<MeaningfulText, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        var problem = TextQuality.problem(value);
        if (problem.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(problem.get().message()).addConstraintViolation();
        return false;
    }
}
