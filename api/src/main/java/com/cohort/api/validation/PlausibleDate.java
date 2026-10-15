package com.cohort.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Not before 2000 and not more than a year ahead, which rules out bad date pickers and typos. */
@Documented
@Constraint(validatedBy = {PlausibleDate.OnDate.class, PlausibleDate.OnInstant.class})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlausibleDate {
    String message() default "Enter a date from the year 2000 onwards and no more than a year ahead.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class OnDate implements ConstraintValidator<PlausibleDate, LocalDate> {
        @Override
        public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
            return value == null || Bounds.isPlausible(value);
        }
    }

    class OnInstant implements ConstraintValidator<PlausibleDate, Instant> {
        @Override
        public boolean isValid(Instant value, ConstraintValidatorContext context) {
            return value == null || Bounds.isPlausible(value.atZone(ZoneOffset.UTC).toLocalDate());
        }
    }

    final class Bounds {
        private static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);

        private Bounds() {
        }

        static boolean isPlausible(LocalDate date) {
            return !date.isBefore(EARLIEST) && !date.isAfter(LocalDate.now(ZoneOffset.UTC).plusYears(1));
        }
    }
}
