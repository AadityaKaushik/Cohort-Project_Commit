package com.cohort.api.dto;

import com.cohort.api.entity.Contact;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** US-95: company and application links must belong to the caller (enforced in {@code ContactService}). */
public record CreateContactRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String roleTitle,
        @Size(max = 320) String email,
        @Size(max = 500) String linkedinUrl,
        Contact.Relationship relationship,
        Contact.ReferralStatus referralStatus,
        UUID companyId,
        UUID applicationId,
        @Size(max = 5000) String notes,
        Instant lastContactedAt) {
}
