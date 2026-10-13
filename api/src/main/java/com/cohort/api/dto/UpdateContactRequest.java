package com.cohort.api.dto;

import com.cohort.api.entity.Contact;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** PATCH semantics: only non-null fields are applied. */
public record UpdateContactRequest(
        @Size(min = 1, max = 200) String name,
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
