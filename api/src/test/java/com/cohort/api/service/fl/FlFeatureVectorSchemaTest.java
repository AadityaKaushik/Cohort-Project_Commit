package com.cohort.api.service.fl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FlFeatureVectorSchemaTest {
    @Test
    void hasExactlyTwentyFourFields() {
        assertThat(FlFeatureVector.class.getRecordComponents()).hasSize(FlFeatureSchema.FEATURE_COUNT);
    }

    @Test
    void everyFieldIsAPrimitiveDouble() {
        for (RecordComponent component : FlFeatureVector.class.getRecordComponents()) {
            assertThat(component.getType())
                    .as("field %s must be a primitive double, never a String/Object", component.getName())
                    .isEqualTo(double.class);
        }
    }

    @Test
    void namedMapKeysExactlyMatchTheAllowListInOrder() {
        FlFeatureVector vector = zeroVector();
        assertThat(vector.toNamedMap().keySet()).containsExactlyElementsOf(FlFeatureSchema.FEATURE_NAMES);
    }

    @Test
    void orderedArrayHasExactlyTwentyFourValues() {
        assertThat(zeroVector().toOrderedArray()).hasSize(FlFeatureSchema.FEATURE_COUNT);
    }

    @Test
    void allowListHasNoDuplicateNames() {
        Set<String> seen = new LinkedHashSet<>(FlFeatureSchema.FEATURE_NAMES);
        assertThat(seen).hasSameSizeAs(FlFeatureSchema.FEATURE_NAMES);
    }

    private FlFeatureVector zeroVector() {
        return new FlFeatureVector(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
