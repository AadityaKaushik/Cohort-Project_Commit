package com.cohort.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CleanNameValidator implements ConstraintValidator<CleanName, String> {
    private static final int MAX_IDENTICAL_RUN = 6;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        if (value.codePoints().anyMatch(cp -> !isAllowed(cp))) {
            return fail(context, "Use letters, numbers and common punctuation only.");
        }
        if (value.codePoints().noneMatch(Character::isLetterOrDigit)) {
            return fail(context, "Include at least one letter or number.");
        }
        if (TextQuality.longestIdenticalRun(value) >= MAX_IDENTICAL_RUN) {
            return fail(context, "This repeats one character many times. Check the spelling.");
        }
        return true;
    }

    private static boolean isAllowed(int cp) {
        if (Character.isLetterOrDigit(cp) || cp == ' ') {
            return true;
        }
        return switch (Character.getType(cp)) {
            case Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK,
                    Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION,
                    Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION,
                    Character.FINAL_QUOTE_PUNCTUATION, Character.OTHER_PUNCTUATION, Character.MATH_SYMBOL,
                    Character.CURRENCY_SYMBOL -> true;
            default -> false;
        };
    }

    private static boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
