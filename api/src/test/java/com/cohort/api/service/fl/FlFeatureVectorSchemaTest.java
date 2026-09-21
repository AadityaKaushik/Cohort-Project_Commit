package com.cohort.api.service.fl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * P51's core privacy guarantee, checked structurally rather than by
 * inspecting one example payload: {@link FlFeatureVector} can only ever
 * carry exactly the 24 allow-listed, bounded {@code double} fields — there
 * is no field type here a string, identifier, or free-text value could
 * ever occupy. "No free text/PII fields leak through" is therefore not
 * just true of today's fields, it is true of every value this record can
 * ever hold.
 */
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
