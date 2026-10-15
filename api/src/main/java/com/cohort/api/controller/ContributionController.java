package com.cohort.api.controller;

import com.cohort.api.dto.ContributionResponse;
import com.cohort.api.dto.CreateContributionRequest;
import com.cohort.api.service.ContributionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** US-96: opt-in anonymous experience contributions. */
@RestController
@RequestMapping("/api/v1/directory/contributions")
public class ContributionController {

    private final ContributionService contributionService;

    public ContributionController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContributionResponse create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateContributionRequest request) {
        return contributionService.create(UUID.fromString(jwt.getSubject()), request);
    }

    @GetMapping("/mine")
    public List<ContributionResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return contributionService.mine(UUID.fromString(jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        contributionService.withdraw(UUID.fromString(jwt.getSubject()), id);
    }
}
