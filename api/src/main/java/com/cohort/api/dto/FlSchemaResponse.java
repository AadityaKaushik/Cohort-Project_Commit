package com.cohort.api.dto;

import java.util.List;

/**
 * US-57: "the opt-in screen states in plain language what is sent and what
 * is not, showing the actual feature schema" — this is that schema,
 * published verbatim from {@code FlFeatureSchema} (the Java-side allow-list,
 * itself kept in sync by hand with {@code fl-service/app/common/schema.py}).
 */
public record FlSchemaResponse(int schemaVersion, int featureCount, List<FeatureBound> features, List<String> notCollected) {

    public record FeatureBound(String name, double lowerBound, double upperBound) {
    }
}
