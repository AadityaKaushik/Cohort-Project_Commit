package com.cohort.api.controller;

import com.cohort.api.dto.CreateOfferRequest;
import com.cohort.api.dto.OfferResponse;
import com.cohort.api.dto.UpdateOfferRequest;
import com.cohort.api.dto.OfferDecisionResponse;
import com.cohort.api.dto.OfferFactorValues;
import com.cohort.api.service.OfferDecisionService;
import com.cohort.api.service.OfferService;
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

@RestController
@RequestMapping("/api/v1/offers")
public class OfferController {

    private final OfferService offerService;
    private final OfferDecisionService decisionService;

    public OfferController(OfferService offerService, OfferDecisionService decisionService) {
        this.offerService = offerService;
        this.decisionService = decisionService;
    }

    @GetMapping
    public List<OfferResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return offerService.list(userId(jwt));
    }

    @GetMapping("/decision")
    public OfferDecisionResponse decision(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(name = "offer_ids", required = false) List<UUID> offerIds) {
        return decisionService.get(userId(jwt), offerIds);
    }

    @PutMapping("/decision/weights")
    public OfferDecisionResponse setWeights(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OfferFactorValues request) {
        return decisionService.setWeights(userId(jwt), request);
    }

    @PutMapping("/{id}/ratings")
    public OfferDecisionResponse setRatings(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody OfferFactorValues request) {
        return decisionService.setRatings(userId(jwt), id, request);
    }

    @GetMapping("/{id}")
    public OfferResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return offerService.get(userId(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfferResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateOfferRequest request) {
        return offerService.create(userId(jwt), request);
    }

    @PatchMapping("/{id}")
    public OfferResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UpdateOfferRequest request) {
        return offerService.update(userId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        offerService.delete(userId(jwt), id);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
