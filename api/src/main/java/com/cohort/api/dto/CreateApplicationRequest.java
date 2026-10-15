package com.cohort.api.dto;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.validation.CleanName;
import com.cohort.api.validation.HttpUrl;
import com.cohort.api.validation.InputText;
import com.cohort.api.validation.MeaningfulText;
import com.cohort.api.validation.PlausibleDate;
import com.cohort.api.validation.SalaryRange;
import com.cohort.api.validation.ValidSalary;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@ValidSalary(currencyRequired = true)
public record CreateApplicationRequest(
        @NotBlank @Size(max = 200) @CleanName String companyName,
        @NotBlank @Size(max = 300) @CleanName String jobTitle,
        String jobDescription,
        @HttpUrl String jobUrl,
        @Size(max = 200) @CleanName String location,
        Application.WorkMode workMode,
        Application.JobType jobType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        @Size(min = 3, max = 3) String salaryCurrency,
        @NotNull @PastOrPresent @PlausibleDate LocalDate dateApplied,
        @NotNull ApplicationStatus status,
        Application.Priority priority,
        @Size(max = 100) @CleanName String source,
        @JsonProperty("is_referral") Boolean isReferral,
        @PlausibleDate LocalDate deadline,
        @Size(max = 200) @CleanName String recruiterName,
        @Email @Size(max = 254) String recruiterEmail,
        @Size(max = 10000) @MeaningfulText String notes,
        String coverLetter) implements SalaryRange {
    public CreateApplicationRequest {
        companyName = InputText.collapse(companyName);
        jobTitle = InputText.collapse(jobTitle);
        jobUrl = jobUrl == null ? null : jobUrl.trim();
        location = InputText.collapse(location);
        source = InputText.collapse(source);
        recruiterName = InputText.collapse(recruiterName);
        salaryCurrency = salaryCurrency == null ? null : salaryCurrency.trim().toUpperCase();
    }
}
