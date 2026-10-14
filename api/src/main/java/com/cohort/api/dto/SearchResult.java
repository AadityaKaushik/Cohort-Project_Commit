package com.cohort.api.dto;

import java.util.UUID;

public record SearchResult(UUID id, String title, String subtitle, UUID parentId) {
}
