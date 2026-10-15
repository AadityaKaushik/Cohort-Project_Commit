package com.cohort.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderResumeSectionsRequest(@NotNull List<UUID> sectionIds) {
}
