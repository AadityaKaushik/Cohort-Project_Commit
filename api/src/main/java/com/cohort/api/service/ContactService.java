package com.cohort.api.service;

import com.cohort.api.dto.ContactResponse;
import com.cohort.api.dto.CreateContactRequest;
import com.cohort.api.dto.UpdateContactRequest;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.Contact;
import com.cohort.api.exception.ApplicationNotFoundException;
import com.cohort.api.exception.CompanyNotFoundException;
import com.cohort.api.exception.ContactNotFoundException;
import com.cohort.api.mapper.ContactMapper;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.ContactRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-95: contacts and referral tracking, strictly scoped to the owner. */
@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;
    private final ContactMapper mapper;

    public ContactService(
            ContactRepository contactRepository,
            CompanyRepository companyRepository,
            ApplicationRepository applicationRepository,
            ContactMapper mapper) {
        this.contactRepository = contactRepository;
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> list(UUID userId, UUID companyId, Contact.ReferralStatus referralStatus) {
        List<Contact> contacts = companyId == null
                ? contactRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                : contactRepository.findAllByUserIdAndCompanyIdOrderByCreatedAtDesc(userId, companyId);
        List<Contact> filtered = referralStatus == null
                ? contacts
                : contacts.stream().filter(contact -> contact.getReferralStatus() == referralStatus).toList();
        return toResponses(userId, filtered);
    }

    @Transactional(readOnly = true)
    public ContactResponse get(UUID userId, UUID id) {
        return toResponses(userId, List.of(requireOwned(userId, id))).get(0);
    }

    @Transactional
    public ContactResponse create(UUID userId, CreateContactRequest request) {
        UUID companyId = resolveCompanyId(userId, request.companyId(), request.applicationId());
        Contact contact = new Contact(userId, request.name().trim());
        contact.setRoleTitle(request.roleTitle());
        contact.setEmail(request.email());
        contact.setLinkedinUrl(request.linkedinUrl());
        if (request.relationship() != null) {
            contact.setRelationship(request.relationship());
        }
        if (request.referralStatus() != null) {
            contact.setReferralStatus(request.referralStatus());
        }
        contact.setCompanyId(companyId);
        contact.setApplicationId(request.applicationId());
        contact.setNotes(request.notes());
        contact.setLastContactedAt(request.lastContactedAt());
        return toResponses(userId, List.of(contactRepository.save(contact))).get(0);
    }

    @Transactional
    public ContactResponse update(UUID userId, UUID id, UpdateContactRequest request) {
        Contact contact = requireOwned(userId, id);
        UUID companyId = resolveCompanyId(userId, request.companyId(), request.applicationId());
        if (request.name() != null) {
            contact.setName(request.name().trim());
        }
        if (request.roleTitle() != null) {
            contact.setRoleTitle(request.roleTitle());
        }
        if (request.email() != null) {
            contact.setEmail(request.email());
        }
        if (request.linkedinUrl() != null) {
            contact.setLinkedinUrl(request.linkedinUrl());
        }
        if (request.relationship() != null) {
            contact.setRelationship(request.relationship());
        }
        if (request.referralStatus() != null) {
            contact.setReferralStatus(request.referralStatus());
        }
        if (companyId != null) {
            contact.setCompanyId(companyId);
        }
        if (request.applicationId() != null) {
            contact.setApplicationId(request.applicationId());
        }
        if (request.notes() != null) {
            contact.setNotes(request.notes());
        }
        if (request.lastContactedAt() != null) {
            contact.setLastContactedAt(request.lastContactedAt());
        }
        return toResponses(userId, List.of(contactRepository.save(contact))).get(0);
    }

    @Transactional
    public ContactResponse markContacted(UUID userId, UUID id) {
        Contact contact = requireOwned(userId, id);
        contact.setLastContactedAt(Instant.now());
        return toResponses(userId, List.of(contactRepository.save(contact))).get(0);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        requireOwned(userId, id);
        contactRepository.deleteByIdAndUserId(id, userId);
    }

    private Contact requireOwned(UUID userId, UUID id) {
        return contactRepository.findByIdAndUserId(id, userId).orElseThrow(ContactNotFoundException::new);
    }

    /** Verifies both links belong to the caller; a linked application supplies the company when none is given. */
    private UUID resolveCompanyId(UUID userId, UUID companyId, UUID applicationId) {
        if (companyId != null && !companyRepository.existsByIdAndUserId(companyId, userId)) {
            throw new CompanyNotFoundException();
        }
        if (applicationId == null) {
            return companyId;
        }
        Application application = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(ApplicationNotFoundException::new);
        return companyId != null ? companyId : application.getCompanyId();
    }

    private List<ContactResponse> toResponses(UUID userId, List<Contact> contacts) {
        Map<UUID, String> companyNames = companyRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(Company::getId, Company::getName, (a, b) -> a, java.util.HashMap::new));
        Instant now = Instant.now();
        return contacts.stream()
                .map(contact -> mapper.toResponse(contact, companyNames.get(contact.getCompanyId()), now))
                .toList();
    }
}
