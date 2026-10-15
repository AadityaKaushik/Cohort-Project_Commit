package com.cohort.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Single-line labels such as company, job title, location and source. */
@Documented
@Constraint(validatedBy = CleanNameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface CleanName {
    String message() default "Use letters, numbers and common punctuation only.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
