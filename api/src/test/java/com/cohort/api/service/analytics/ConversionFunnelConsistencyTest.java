package com.cohort.api.service.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.dto.ConversionRatesResponse;
import com.cohort.api.dto.FunnelResponse;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationStatusHistory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Conversion cards and the funnel are two views of one dataset: they must agree on every count. */
class ConversionFunnelConsistencyTest {

    private final AnalyticsSummaryCalculator calculator = new AnalyticsSummaryCalculator();

    private ApplicationSnapshot snapshot(ApplicationStatus current, ApplicationStatus... path) {
        UUID id = UUID.randomUUID();
        List<ApplicationStatusHistory> history = new ArrayList<>();
        ApplicationStatus previous = null;
        for (ApplicationStatus step : path) {
            history.add(new ApplicationStatusHistory(id, previous, step, null));
            previous = step;
        }
        return new ApplicationSnapshot(id, current, current, Instant.now(), history);
    }

    @Test
    void numeratorsAreFunnelCountsAndDenominatorsPlusOpenAreTheSourceStageCounts() {
        List<ApplicationSnapshot> snapshots = List.of(
                snapshot(ApplicationStatus.APPLIED, ApplicationStatus.SAVED, ApplicationStatus.APPLIED),
                snapshot(ApplicationStatus.REJECTED, ApplicationStatus.SAVED, ApplicationStatus.APPLIED,
                        ApplicationStatus.REJECTED),
                snapshot(ApplicationStatus.OA, ApplicationStatus.SAVED, ApplicationStatus.APPLIED,
                        ApplicationStatus.OA),
                snapshot(ApplicationStatus.INTERVIEW, ApplicationStatus.SAVED, ApplicationStatus.APPLIED,
                        ApplicationStatus.OA, ApplicationStatus.INTERVIEW),
                snapshot(ApplicationStatus.OFFER, ApplicationStatus.SAVED, ApplicationStatus.APPLIED,
                        ApplicationStatus.OA, ApplicationStatus.INTERVIEW, ApplicationStatus.FINAL_ROUND,
                        ApplicationStatus.OFFER),
                snapshot(ApplicationStatus.REJECTED, ApplicationStatus.SAVED, ApplicationStatus.REJECTED),
                snapshot(ApplicationStatus.SAVED, ApplicationStatus.SAVED));

        FunnelResponse funnel = calculator.funnel(snapshots);
        ConversionRatesResponse rates = calculator.conversions(snapshots);

        assertThat(rates.applicationToOa().numerator()).isEqualTo(funnel.reachedOa());
        assertThat(rates.oaToInterview().numerator()).isEqualTo(funnel.reachedInterview());
        assertThat(rates.interviewToFinal().numerator()).isEqualTo(funnel.reachedFinal());
        assertThat(rates.finalToOffer().numerator()).isEqualTo(funnel.reachedOffer());
        assertThat(rates.overallSuccess().numerator()).isEqualTo(funnel.reachedOffer());

        assertThat(rates.applicationToOa().denominator() + rates.applicationToOa().stillOpen())
                .isEqualTo(funnel.applied());
        assertThat(rates.oaToInterview().denominator() + rates.oaToInterview().stillOpen())
                .isEqualTo(funnel.reachedOa());
        assertThat(rates.interviewToFinal().denominator() + rates.interviewToFinal().stillOpen())
                .isEqualTo(funnel.reachedInterview());
        assertThat(rates.finalToOffer().denominator() + rates.finalToOffer().stillOpen())
                .isEqualTo(funnel.reachedFinal());
    }
}
