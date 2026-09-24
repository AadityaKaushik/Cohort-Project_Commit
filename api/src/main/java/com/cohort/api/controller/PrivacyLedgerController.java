package com.cohort.api.controller;

import com.cohort.api.dto.PrivacyLedgerEntryResponse;
import com.cohort.api.service.fl.PrivacyLedgerService;
import com.cohort.api.service.fl.PrivacyLedgerService.PrivacyLedgerEntryView;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US-63: read-only. Entries are written only by {@code PrivacyLedgerService} from a real fl-service round response, never through this controller. */
@RestController
@RequestMapping("/api/v1/fl/privacy-ledger")
public class PrivacyLedgerController {

    private final PrivacyLedgerService privacyLedgerService;

    public PrivacyLedgerController(PrivacyLedgerService privacyLedgerService) {
        this.privacyLedgerService = privacyLedgerService;
    }

    @GetMapping
    public List<PrivacyLedgerEntryResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return privacyLedgerService.listForUser(userId(jwt)).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PrivacyLedgerEntryResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return toResponse(privacyLedgerService.getForUser(id, userId(jwt)));
    }

    private PrivacyLedgerEntryResponse toResponse(PrivacyLedgerEntryView view) {
        return new PrivacyLedgerEntryResponse(
                view.entry().getId(),
                view.roundNumber(),
                view.entry().getSchemaVersion(),
                view.entry().getPreClipNorm(),
                view.entry().getPostClipNorm(),
                view.entry().getNoiseMultiplier(),
                view.entry().getCohortSize(),
                view.entry().getEpsilon(),
                view.entry().getTransmittedVector(),
                view.entry().getCreatedAt());
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
