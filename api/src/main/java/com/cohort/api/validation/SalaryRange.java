package com.cohort.api.validation;

import java.math.BigDecimal;

public interface SalaryRange {
    BigDecimal salaryMin();

    BigDecimal salaryMax();

    String salaryCurrency();
}
