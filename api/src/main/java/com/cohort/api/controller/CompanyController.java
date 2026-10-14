package com.cohort.api.controller;

import com.cohort.api.dto.CompanyDetailResponse;
import com.cohort.api.dto.CompanyListItem;
import com.cohort.api.dto.CompanyNoteRequest;
import com.cohort.api.dto.CompanyNoteResponse;
import com.cohort.api.dto.UpdateCompanyRequest;
import com.cohort.api.service.CompanyService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** US-96: company pages. */
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public List<CompanyListItem> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String q) {
        return companyService.list(userId(jwt), q);
    }

    @GetMapping("/{id}")
    public CompanyDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return companyService.get(userId(jwt), id);
    }

    @PatchMapping("/{id}")
    public CompanyDetailResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UpdateCompanyRequest request) {
        return companyService.update(userId(jwt), id, request);
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyNoteResponse addNote(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody CompanyNoteRequest request) {
        return companyService.addNote(userId(jwt), id, request);
    }

    @PutMapping("/{id}/notes/{noteId}")
    public CompanyNoteResponse updateNote(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID noteId,
            @Valid @RequestBody CompanyNoteRequest request) {
        return companyService.updateNote(userId(jwt), id, noteId, request);
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID noteId) {
        companyService.deleteNote(userId(jwt), id, noteId);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
