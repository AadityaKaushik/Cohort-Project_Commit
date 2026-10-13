package com.cohort.api.controller;

import com.cohort.api.dto.ContactResponse;
import com.cohort.api.dto.CreateContactRequest;
import com.cohort.api.dto.UpdateContactRequest;
import com.cohort.api.entity.Contact;
import com.cohort.api.service.ContactService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** US-95: contacts and referrals. Thin by design (ARCHITECTURE_REVISION_JAVA.md §4.2). */
@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public List<ContactResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "company_id", required = false) UUID companyId,
            @RequestParam(name = "referral_status", required = false) Contact.ReferralStatus referralStatus) {
        return contactService.list(userId(jwt), companyId, referralStatus);
    }

    @GetMapping("/{id}")
    public ContactResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return contactService.get(userId(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateContactRequest request) {
        return contactService.create(userId(jwt), request);
    }

    @PatchMapping("/{id}")
    public ContactResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UpdateContactRequest request) {
        return contactService.update(userId(jwt), id, request);
    }

    @PostMapping("/{id}/contacted")
    public ContactResponse markContacted(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return contactService.markContacted(userId(jwt), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        contactService.delete(userId(jwt), id);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
