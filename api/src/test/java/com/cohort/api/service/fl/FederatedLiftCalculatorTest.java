package com.cohort.api.service.fl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FederatedLiftCalculatorTest {
    private final FederatedLiftCalculator calculator = new FederatedLiftCalculator();

    @Test
    void aZeroModelPredictsAnEvenChanceAndChangesNothing() {
        double confidence = calculator.predictedConfidence(model(Collections.nCopies(25, 0.0)), features(0.7));

        assertThat(confidence).isEqualTo(0.5);
        assertThat(calculator.adjustmentMultiplier(confidence, 0.9)).isEqualTo(1.0);
    }

    @Test
    void predictionIsTheLogisticOfWeightsTimesFeaturesPlusIntercept() {
        List<Double> parameters = new ArrayList<>(Collections.nCopies(24, 0.0));
        parameters.set(0, 2.0);
        parameters.add(-0.5);

        double confidence = calculator.predictedConfidence(model(parameters), features(0.5));

        assertThat(confidence).isCloseTo(1.0 / (1.0 + Math.exp(-(2.0 * 0.5 - 0.5))), org.assertj.core.data.Offset.offset(1e-12));
    }

    @Test
    void lowPredictedConfidenceMovesSeverePrepItemsUpMoreThanMildOnes() {
        double severe = calculator.adjustmentMultiplier(0.2, 0.9);
        double mild = calculator.adjustmentMultiplier(0.2, 0.1);

        assertThat(severe).isGreaterThan(mild).isGreaterThan(1.0);
    }

    @Test
    void highPredictedConfidenceMovesSevereItemsDownALittle() {
        assertThat(calculator.adjustmentMultiplier(0.9, 0.9)).isLessThan(1.0);
    }

    @Test
    void theMultiplierStaysInsideItsBoundsForAnyPredictionAndSeverity() {
        for (double confidence = 0.0; confidence <= 1.0; confidence += 0.1) {
            for (double severity = 0.0; severity <= 1.0; severity += 0.1) {
                assertThat(calculator.adjustmentMultiplier(confidence, severity))
                        .isBetween(FederatedLiftCalculator.MIN_MULTIPLIER, FederatedLiftCalculator.MAX_MULTIPLIER);
            }
        }
        assertThat(calculator.adjustmentMultiplier(-50.0, 50.0)).isEqualTo(FederatedLiftCalculator.MAX_MULTIPLIER);
        assertThat(calculator.adjustmentMultiplier(50.0, 50.0)).isEqualTo(FederatedLiftCalculator.MIN_MULTIPLIER);
    }

    @Test
    void aBrokenOrWrongSizedModelFallsBackToNeutral() {
        assertThat(calculator.predictedConfidence(model(List.of(1.0, 2.0)), features(0.5))).isEqualTo(0.5);
        assertThat(calculator.predictedConfidence(model(Collections.nCopies(25, Double.NaN)), features(0.5))).isEqualTo(0.5);
        assertThat(calculator.adjustmentMultiplier(Double.NaN, 0.5)).isEqualTo(1.0);
    }

    private static FlModelInfo model(List<Double> parameters) {
        return new FlModelInfo(1, 1, 3, Instant.EPOCH, parameters);
    }

    private static FlFeatureVector features(double value) {
        return new FlFeatureVector(
                value, value, value, value, value, value, value, value, value, value, value, value,
                value, value, value, value, value, value, value, value, value, value, value, value);
    }
}
