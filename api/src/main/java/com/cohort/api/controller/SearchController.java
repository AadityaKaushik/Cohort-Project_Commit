package com.cohort.api.controller;

import com.cohort.api.dto.SearchResponse;
import com.cohort.api.service.SearchService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponse search(@AuthenticationPrincipal Jwt jwt, @RequestParam(name = "q", defaultValue = "") String query) {
        return searchService.search(UUID.fromString(jwt.getSubject()), query);
    }
}
