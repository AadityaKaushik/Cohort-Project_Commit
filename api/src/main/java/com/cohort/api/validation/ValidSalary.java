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
import java.math.BigDecimal;
import java.util.Currency;

/** Bounds and ordering for a salary range. Currency is only required when {@code currencyRequired} is set. */
@Documented
@Constraint(validatedBy = ValidSalary.Validator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSalary {
    String message() default "Check the salary range.";

    boolean currencyRequired() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidSalary, SalaryRange> {
        static final BigDecimal MAX = new BigDecimal("100000000");

        private boolean currencyRequired;

        @Override
        public void initialize(ValidSalary annotation) {
            currencyRequired = annotation.currencyRequired();
        }

        @Override
        public boolean isValid(SalaryRange range, ConstraintValidatorContext context) {
            if (range == null) {
                return true;
            }
            boolean valid = true;
            context.disableDefaultConstraintViolation();
            valid &= checkBound(range.salaryMin(), "salaryMin", context);
            valid &= checkBound(range.salaryMax(), "salaryMax", context);
            if (valid && range.salaryMin() != null && range.salaryMax() != null
                    && range.salaryMin().compareTo(range.salaryMax()) > 0) {
                report(context, "salaryMax", "The maximum cannot be lower than the minimum.");
                valid = false;
            }
            String currency = range.salaryCurrency();
            boolean hasAmount = range.salaryMin() != null || range.salaryMax() != null;
            if (currency != null && !currency.isEmpty() && !isKnownCurrency(currency)) {
                report(context, "salaryCurrency", "Use a three-letter currency code such as INR, USD or EUR.");
                valid = false;
            } else if (currencyRequired && hasAmount && (currency == null || currency.isEmpty())) {
                report(context, "salaryCurrency", "Add the currency for this salary, for example INR or USD.");
                valid = false;
            }
            return valid;
        }

        private static boolean checkBound(BigDecimal value, String field, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            if (value.signum() < 0 || value.compareTo(MAX) > 0) {
                report(context, field, "Enter an amount between 0 and 100,000,000.");
                return false;
            }
            return true;
        }

        private static boolean isKnownCurrency(String code) {
            try {
                Currency.getInstance(code.toUpperCase());
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        private static void report(ConstraintValidatorContext context, String field, String message) {
            context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
        }
    }
}
