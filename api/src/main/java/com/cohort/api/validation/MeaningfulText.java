package com.cohort.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Free text that is either absent or passes the hard checks in {@link TextQuality}. */
@Documented
@Constraint(validatedBy = MeaningfulTextValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MeaningfulText {
    String message() default "Write a few real words, or leave this blank.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
