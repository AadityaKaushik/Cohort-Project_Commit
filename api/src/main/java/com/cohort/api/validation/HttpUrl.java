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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

@Documented
@Constraint(validatedBy = HttpUrl.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpUrl {
    String message() default "Enter a full link that starts with http:// or https://, like https://example.com/jobs/123.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<HttpUrl, String> {
        private static final int MAX_LENGTH = 2048;

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return true;
            }
            String candidate = value.trim();
            if (candidate.length() > MAX_LENGTH) {
                return false;
            }
            try {
                URI uri = new URI(candidate);
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
                return (scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null;
            } catch (URISyntaxException e) {
                return false;
            }
        }
    }
}
