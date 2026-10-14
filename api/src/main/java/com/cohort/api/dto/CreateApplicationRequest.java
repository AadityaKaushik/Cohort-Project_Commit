package com.cohort.api.dto;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateApplicationRequest(
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 300) String jobTitle,
        String jobDescription,
        String jobUrl,
        String location,
        Application.WorkMode workMode,
        Application.JobType jobType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        @Size(min = 3, max = 3) String salaryCurrency,
        @NotNull @PastOrPresent LocalDate dateApplied,
        @NotNull ApplicationStatus status,
        Application.Priority priority,
        String source,
        @JsonProperty("is_referral") Boolean isReferral,
        LocalDate deadline,
        String recruiterName,
        String recruiterEmail,
        String notes,
        String coverLetter) {
}
