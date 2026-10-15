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

/** Text with no email address or phone number. Used where the text is shown to other users. */
@Documented
@Constraint(validatedBy = NoContactDetails.Check.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoContactDetails {
    String message() default "Remove email addresses and phone numbers. This text is shown to other candidates.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Check implements ConstraintValidator<NoContactDetails, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null
                    || !TextQuality.hints(value, true).contains(TextQuality.Hint.CONTAINS_CONTACT_DETAILS);
        }
    }
}
