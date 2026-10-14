package com.cohort.api.service.fl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class FederatedLiftCalculatorTest {
    private final FederatedLiftCalculator calculator = new FederatedLiftCalculator();

    @Test
    void neverExceedsTheUpperBoundEvenForAnExtremeModel() {
        FlModelInfo hugeModel = modelWithParameters(1000.0);

        double multiplier = calculator.adjustmentMultiplier(hugeModel, 1.0);

        assertThat(multiplier).isEqualTo(FederatedLiftCalculator.MAX_MULTIPLIER);
    }

    @Test
    void neverGoesBelowTheLowerBoundEvenForAnExtremeModel() {
        FlModelInfo hugeModel = modelWithParameters(1000.0);

        double multiplier = calculator.adjustmentMultiplier(hugeModel, 0.0);

        assertThat(multiplier).isEqualTo(FederatedLiftCalculator.MIN_MULTIPLIER);
    }

    @Test
    void staysWithinBoundsAcrossARangeOfRealisticInputs() {
        for (double param = -5.0; param <= 5.0; param += 0.37) {
            for (double severity = 0.0; severity <= 1.0; severity += 0.11) {
                double multiplier = calculator.adjustmentMultiplier(modelWithParameters(param), severity);
                assertThat(multiplier)
                        .as("param=%s severity=%s", param, severity)
                        .isBetween(FederatedLiftCalculator.MIN_MULTIPLIER, FederatedLiftCalculator.MAX_MULTIPLIER);
            }
        }
    }

    @Test
    void aNeutralModelWithSeverityAtTheMidpointProducesNoAdjustment() {
        FlModelInfo zeroModel = modelWithParameters(0.0);

        assertThat(calculator.adjustmentMultiplier(zeroModel, 0.5)).isEqualTo(1.0);
    }

    @Test
    void differentCandidateSeveritiesProduceDifferentMultipliersForTheSameModel() {
        FlModelInfo model = modelWithParameters(0.4);

        double low = calculator.adjustmentMultiplier(model, 0.1);
        double high = calculator.adjustmentMultiplier(model, 0.9);

        assertThat(low).isNotEqualTo(high);
    }

    @Test
    void aNonFiniteModelNeverEscapesTheBounds() {
        for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            double multiplier = calculator.adjustmentMultiplier(modelWithParameters(bad), 0.9);
            assertThat(multiplier)
                    .as("param=%s", bad)
                    .isBetween(FederatedLiftCalculator.MIN_MULTIPLIER, FederatedLiftCalculator.MAX_MULTIPLIER);
        }
    }

    @Test
    void anEmptyParameterVectorIsNeutral() {
        FlModelInfo empty = new FlModelInfo(1, 1, 3, Instant.now(), List.of());

        assertThat(calculator.adjustmentMultiplier(empty, 1.0)).isEqualTo(1.0);
    }

    private FlModelInfo modelWithParameters(double value) {
        return new FlModelInfo(1, 1, 3, Instant.now(), List.of(value, value, value));
    }
}
