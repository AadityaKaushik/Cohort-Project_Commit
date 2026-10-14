package com.cohort.api.dto;

import java.util.List;

public record FlSchemaResponse(int schemaVersion, int featureCount, List<FeatureBound> features, List<String> notCollected) {
    public record FeatureBound(String name, double lowerBound, double upperBound) {
    }
}
