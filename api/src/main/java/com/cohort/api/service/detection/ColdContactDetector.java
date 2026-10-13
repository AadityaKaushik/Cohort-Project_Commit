package com.cohort.api.service.detection;

import com.cohort.api.entity.Company;
import com.cohort.api.entity.Contact;
import com.cohort.api.event.ColdContactEvent;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.ContactRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Observer pattern, subject side (US-95): publishes a {@link ColdContactEvent}
 * for every contact the user has not touched for {@link #COLD_AFTER}. Like the
 * other detectors it depends only on repositories and the generic event
 * publisher, and never learns who subscribes.
 */
@Component
public class ColdContactDetector {

    public static final Duration COLD_AFTER = Duration.ofDays(30);

    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ColdContactDetector(
            ContactRepository contactRepository,
            CompanyRepository companyRepository,
            ApplicationEventPublisher eventPublisher) {
        this.contactRepository = contactRepository;
        this.companyRepository = companyRepository;
        this.eventPublisher = eventPublisher;
    }

    public static boolean isCold(Contact contact, Instant now) {
        if (contact.getReferralStatus() == Contact.ReferralStatus.declined) {
            return false;
        }
        Instant lastTouchedAt = contact.lastTouchedAt();
        return lastTouchedAt != null && !lastTouchedAt.isAfter(now.minus(COLD_AFTER));
    }

    @Transactional(readOnly = true)
    public int detect(UUID userId, Instant now) {
        List<Contact> cold = contactRepository.findAllByUserId(userId).stream()
                .filter(contact -> isCold(contact, now))
                .toList();
        if (cold.isEmpty()) {
            return 0;
        }
        Map<UUID, Company> companiesById = companyRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(Company::getId, Function.identity()));
        for (Contact contact : cold) {
            Company company = contact.getCompanyId() == null ? null : companiesById.get(contact.getCompanyId());
            boolean referralPending = contact.getReferralStatus() == Contact.ReferralStatus.requested
                    || contact.getReferralStatus() == Contact.ReferralStatus.agreed;
            eventPublisher.publishEvent(new ColdContactEvent(
                    userId,
                    contact.getId(),
                    contact.getName(),
                    company == null ? null : company.getName(),
                    referralPending,
                    contact.lastTouchedAt(),
                    Duration.between(contact.lastTouchedAt(), now).toDays()));
        }
        return cold.size();
    }
}
