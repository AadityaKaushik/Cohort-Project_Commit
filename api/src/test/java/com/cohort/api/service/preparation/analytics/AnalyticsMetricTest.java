package com.cohort.api.service.preparation.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * P29's mandatory verify (BUILD_PHASES_JAVA.md): proves a concrete metric
 * subclass cannot bypass the sample-size gate. The adversarial subclasses
 * below deliberately try every seam a careless or malicious implementer
 * might reach for — returning a confident value from a tiny sample,
 * formatting the output as if it were trustworthy — and none of it moves
 * {@code sufficientData}, because that field is computed and assigned only
 * inside {@link AnalyticsMetric#evaluate}, from {@code sampleSize()} and
 * {@code minimumSampleSize()}, never from {@code compute()}/{@code format()}'s
 * return values.
 */
class AnalyticsMetricTest {

    /** An honest metric: sample size is the input's length, value is its average. */
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

    /**
     * The adversarial subclass: honestly reports a tiny sample size (it
     * has no other choice — {@code sampleSize} is abstract and must
     * reflect the real input), but its {@code compute} and {@code format}
     * steps try to present the result as confident regardless. If the
     * Template Method skeleton had any seam for a subclass to influence
     * {@code sufficientData}, this is where it would be exploited.
     */
    private static final class OverconfidentMetric extends AnalyticsMetric<List<Integer>, Double> {
        @Override
        protected int sampleSize(List<Integer> input) {
            return input.size(); // honest -- it has no way to lie about this and still be useful
        }

        @Override
        protected Double compute(List<Integer> input) {
            // Tries to "cheat" by returning a suspiciously precise, confident-looking value from almost no data.
            return 0.987654321;
        }

        @Override
        protected String format(Double value, boolean sufficientData) {
            // Tries to ignore the sufficientData hint entirely and always claim confidence.
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
        List<Integer> tinySample = List.of(9); // 1 observation, well under DEFAULT_MINIMUM_SAMPLE_SIZE (5)

        AnalyticsResult<Double> honest = new AverageMetric().evaluate(tinySample);
        AnalyticsResult<Double> adversarial = new OverconfidentMetric().evaluate(tinySample);

        assertThat(honest.sampleSize()).isEqualTo(1);
        assertThat(honest.sufficientData()).isFalse();

        // The adversarial subclass's compute()/format() tried to look confident; the gate still caught it.
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

    /** A metric may legitimately declare its own threshold (a documented hook) -- that is a different, honest gate, not a bypass of the base class's enforcement mechanism. */
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
        List<Integer> sampleOfFive = List.of(1, 2, 3, 4, 5); // sufficient for the 5-default, not for this metric's own 10

        AnalyticsResult<Double> result = new StrictMetric().evaluate(sampleOfFive);

        assertThat(result.sampleSize()).isEqualTo(5);
        assertThat(result.sufficientData()).isFalse();
    }
}
