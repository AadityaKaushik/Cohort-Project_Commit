package com.cohort.api.service.preparation.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyticsMetricTest {
    private static final class AverageMetric extends AnalyticsMetric<List<Integer>, Double> {
        @Override
        protected int sampleSize(List<Integer> input) {
            return input.size();
        }

        @Override
        protected Double compute(List<Integer> input) {
            return input.stream().mapToInt(Integer::intValue).average().orElse(0);
        }
    }

    private static final class OverconfidentMetric extends AnalyticsMetric<List<Integer>, Double> {
        @Override
        protected int sampleSize(List<Integer> input) {
            return input.size();
        }

        @Override
        protected Double compute(List<Integer> input) {
            return 0.987654321;
        }

        @Override
        protected String format(Double value, boolean sufficientData) {
            return "HIGH CONFIDENCE: " + value;
        }
    }

    @Test
    void evaluateIsFinalSoTheSkeletonItselfCannotBeOverridden() throws NoSuchMethodException {
        Method evaluate = AnalyticsMetric.class.getMethod("evaluate", Object.class);
        assertThat(Modifier.isFinal(evaluate.getModifiers()))
                .as("AnalyticsMetric.evaluate must be final -- the Template Method skeleton is not a subclass's to rewrite")
                .isTrue();
    }

    @Test
    void aSubclassBelowTheSampleSizeThresholdIsMarkedInsufficientRegardlessOfWhatComputeReturns() {
        List<Integer> tinySample = List.of(9);

        AnalyticsResult<Double> honest = new AverageMetric().evaluate(tinySample);
        AnalyticsResult<Double> adversarial = new OverconfidentMetric().evaluate(tinySample);

        assertThat(honest.sampleSize()).isEqualTo(1);
        assertThat(honest.sufficientData()).isFalse();

        assertThat(adversarial.sampleSize()).isEqualTo(1);
        assertThat(adversarial.value()).isEqualTo(0.987654321);
        assertThat(adversarial.formatted()).contains("HIGH CONFIDENCE");
        assertThat(adversarial.sufficientData())
                .as("a subclass's compute()/format() output must never be able to flip sufficientData to true")
                .isFalse();
    }

    @Test
    void aSubclassAtOrAboveTheThresholdIsMarkedSufficient() {
        List<Integer> adequateSample = List.of(1, 2, 3, 4, 5);

        AnalyticsResult<Double> result = new AverageMetric().evaluate(adequateSample);

        assertThat(result.sampleSize()).isEqualTo(5);
        assertThat(result.sufficientData()).isTrue();
        assertThat(result.value()).isEqualTo(3.0);
    }

    private static final class StrictMetric extends AnalyticsMetric<List<Integer>, Double> {
        @Override
        protected int sampleSize(List<Integer> input) {
            return input.size();
        }

        @Override
        protected Double compute(List<Integer> input) {
            return input.stream().mapToInt(Integer::intValue).average().orElse(0);
        }

        @Override
        protected int minimumSampleSize() {
            return 10;
        }
    }

    @Test
    void aSubclassMayOverrideTheThresholdButTheGateStillAppliesAtThatThreshold() {
        List<Integer> sampleOfFive = List.of(1, 2, 3, 4, 5);

        AnalyticsResult<Double> result = new StrictMetric().evaluate(sampleOfFive);

        assertThat(result.sampleSize()).isEqualTo(5);
        assertThat(result.sufficientData()).isFalse();
    }
}
