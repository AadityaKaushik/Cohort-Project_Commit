package com.cohort.api.dto;

import com.cohort.api.entity.Application;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

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
