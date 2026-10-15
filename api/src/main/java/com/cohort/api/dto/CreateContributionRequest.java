package com.cohort.api.dto;

import com.cohort.api.entity.ExperienceContribution.Level;
import com.cohort.api.entity.ExperienceContribution.Outcome;
import com.cohort.api.validation.CleanName;
import com.cohort.api.validation.MeaningfulText;
import com.cohort.api.validation.NoContactDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Either companySlug (a directory company) or companyName (creates a stub profile). */
public record CreateContributionRequest(
        @Size(max = 200) String companySlug,
        @Size(max = 200) @CleanName String companyName,
        @NotNull
        @Pattern(regexp = "software_engineer|data_scientist|product_manager|consulting_analyst|investment_banking_analyst")
        String roleFamily,
        @NotNull Level level,
        @Min(2000) @Max(2100) int year,
        @NotEmpty @Size(max = 8) @Valid List<ContributionRoundInput> rounds,
        @Min(1) @Max(5) int overallDifficulty,
        @Min(1) @Max(5) int overallExperience,
        @NotNull Outcome outcome,
        @Size(max = 280) @MeaningfulText @NoContactDetails String tip) {
}
