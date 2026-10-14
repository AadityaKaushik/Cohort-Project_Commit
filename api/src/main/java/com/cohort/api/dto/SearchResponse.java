package com.cohort.api.dto;

import java.util.List;

public record SearchResponse(
        List<SearchResult> applications,
        List<SearchResult> companies,
        List<SearchResult> questions,
        List<SearchResult> notes) {
}
