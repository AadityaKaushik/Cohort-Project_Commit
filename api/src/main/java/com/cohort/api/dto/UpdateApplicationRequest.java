package com.cohort.api.dto;

import com.cohort.api.entity.Application;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Partial update (FR-18): a null field means "leave unchanged" (same
 * convention as UpdateProfileRequest). Deliberately has no {@code status}
 * field — status only ever changes through {@code POST /applications/{id}/status},
 * which is the one path that runs it past the State pattern and records
 * history (FR-20, FR-21). Allowing a direct status write here would let a
 * client bypass both.
 */
public record UpdateApplicationRequest(
        @Size(max = 200) String companyName,
        @Size(max = 300) String jobTitle,
        String jobDescription,
        String jobUrl,
        String location,
        Application.WorkMode workMode,
        Application.JobType jobType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        @Size(min = 3, max = 3) String salaryCurrency,
        @PastOrPresent LocalDate dateApplied,
        Application.Priority priority,
        String source,
        @JsonProperty("is_referral") Boolean isReferral,
        LocalDate deadline,
        String recruiterName,
        String recruiterEmail,
        String notes,
        String coverLetter) {
}
