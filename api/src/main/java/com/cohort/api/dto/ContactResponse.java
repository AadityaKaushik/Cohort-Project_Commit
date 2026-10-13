package com.cohort.api.dto;

import com.cohort.api.entity.Contact;
import java.time.Instant;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        String name,
        String roleTitle,
        String email,
        String linkedinUrl,
        Contact.Relationship relationship,
        Contact.ReferralStatus referralStatus,
        UUID companyId,
        String companyName,
        UUID applicationId,
        String notes,
        Instant lastContactedAt,
        boolean cold,
        Instant createdAt,
        Instant updatedAt) {
}
