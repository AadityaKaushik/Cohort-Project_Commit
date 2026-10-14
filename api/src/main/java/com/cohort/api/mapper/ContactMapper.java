package com.cohort.api.mapper;

import com.cohort.api.dto.ContactResponse;
import com.cohort.api.entity.Contact;
import com.cohort.api.service.detection.ColdContactDetector;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ContactMapper {

    public ContactResponse toResponse(Contact contact, String companyName, Instant now) {
        return new ContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getRoleTitle(),
                contact.getEmail(),
                contact.getLinkedinUrl(),
                contact.getRelationship(),
                contact.getReferralStatus(),
                contact.getCompanyId(),
                companyName,
                contact.getApplicationId(),
                contact.getNotes(),
                contact.getLastContactedAt(),
                ColdContactDetector.isCold(contact, now),
                contact.getCreatedAt(),
                contact.getUpdatedAt());
    }
}
