package com.cohort.api.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.analytics.AnalyticsSummaryCalculator;
import com.cohort.api.service.analytics.ApplicationSnapshot;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * P63's verify (Builder): the report {@link ReportBuilder} assembles from
 * {@code fixtures/reports/case1-input.json} matches the expected output
 * fixtures byte for byte once rendered to CSV. The expected numbers were
 * checked by hand against PROJECT_PLAN.md §20.7 (not merely captured from a
 * run): with all seven applications, 5 are resolved at Applied and 4 of
 * those reached at least the online assessment (an interview implies it), so
 * applied-to-OA is 4/5 = 80.0%; only 1 offer, so overall is 1/5 = 20.0%;
 * every other rate has fewer than 5 resolved applications and is withheld.
 */
class ReportBuilderTest {

    private final ReportFixtures.Case fixture = ReportFixtures.load("case1");
    private final CsvReportRenderer csv = new CsvReportRenderer();

    private ReportBuilder builder() {
        return ReportBuilder.forOwner(fixture.owner(), fixture.applications()).generatedAt(fixture.generatedAt(), ZoneOffset.UTC);
    }

    @Test
    void applicationsFilteredByDateRangeMatchTheFixture() {
        Report report = builder()
                .withDateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 15))
                .withApplications()
                .build();

        assertThat(asText(csv.render(report))).isEqualTo(ReportFixtures.expected("case1-applications-2026-07-01-to-2026-09-15.csv"));
        assertThat(report.title()).isEqualTo("Applications report");
        assertThat(report.includesAnalytics()).isFalse();
    }

    @Test
    void analyticsSummaryMatchesTheFixture() {
        Report report = builder().withAnalytics().build();

        assertThat(asText(csv.render(report))).isEqualTo(ReportFixtures.expected("case1-analytics-summary.csv"));
        assertThat(report.title()).isEqualTo("Analytics summary");
        assertThat(report.includesApplications()).isFalse();
    }

    @Test
    void theHandComputedFiguresBehindTheFixtureHold() {
        Report.AnalyticsSummary analytics = builder().withAnalytics().build().analytics();

        assertThat(analytics.applicationCount()).isEqualTo(7);
        assertThat(analytics.funnel().applied()).isEqualTo(7);
        assertThat(analytics.funnel().reachedOa()).isEqualTo(4);
        assertThat(analytics.funnel().reachedInterview()).isEqualTo(3);
        assertThat(analytics.funnel().reachedOffer()).isEqualTo(1);
        assertThat(analytics.conversions().applicationToOa().numerator()).isEqualTo(4);
        assertThat(analytics.conversions().applicationToOa().denominator()).isEqualTo(5);
        assertThat(analytics.conversions().applicationToOa().formatted()).isEqualTo("80.0%");
        assertThat(analytics.conversions().overallSuccess().formatted()).isEqualTo("20.0%");
        assertThat(analytics.conversions().oaToInterview().sufficientData()).isFalse();
        assertThat(analytics.dwellTimes().byStage().get(ApplicationStatus.APPLIED).medianDays()).isEqualTo(5.0);
        assertThat(analytics.rejections().noResponseCount()).isEqualTo(1);
        assertThat(analytics.statusBreakdown()).containsEntry(ApplicationStatus.APPLIED, 2L).containsEntry(ApplicationStatus.REJECTED, 2L);
    }

    @Test
    void theAnalyticsSectionIsExactlyWhatTheInAppCalculatorComputesForTheSameRows() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 9, 15);
        Report.AnalyticsSummary analytics = builder().withDateRange(from, to).withAnalytics().build().analytics();

        List<ApplicationSnapshot> inRange = fixture.applications().stream()
                .filter(input -> !input.dateApplied().isBefore(from) && !input.dateApplied().isAfter(to))
                .map(ReportApplicationInput::snapshot)
                .toList();
        AnalyticsSummaryCalculator calculator = new AnalyticsSummaryCalculator();
        assertThat(analytics.applicationCount()).isEqualTo(4);
        assertThat(analytics.funnel()).isEqualTo(calculator.funnel(inRange));
        assertThat(analytics.conversions()).isEqualTo(calculator.conversions(inRange));
        assertThat(analytics.dwellTimes()).isEqualTo(calculator.dwellTimes(inRange));
        assertThat(analytics.rejections()).isEqualTo(calculator.rejectionDistribution(inRange, fixture.generatedAt()));
    }

    @Test
    void aProgressReportRendersAsAReadableMultiSectionPdf() throws Exception {
        Report report = builder().withApplications().withAnalytics().build();

        byte[] pdf = new PdfReportRenderer().render(report);

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        PdfReader reader = new PdfReader(pdf);
        try {
            assertThat(reader.getNumberOfPages()).isEqualTo(2);
            assertThat(reader.getInfo()).containsEntry("Title", "Job search progress report");
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String firstPage = extractor.getTextFromPage(1);
            String secondPage = extractor.getTextFromPage(2);
            assertThat(firstPage)
                    .contains("Job search progress report", "Prepared for Asha Rao", "All applications",
                            "Applications (7)", "Acme Corp (referral)", "SRE, Payments", "Rejected (after", "Page 1");
            assertThat(secondPage)
                    .contains("Analytics summary", "Conversion rates", "80.0%", "insufficient data", "4 of 5",
                            "Median days in each stage", "No response (60+ days, still open)", "Page 2");
        } finally {
            reader.close();
        }
    }

    @Test
    void anEmptyDateRangeStillProducesAClearReport() throws Exception {
        Report report = builder().withDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)).withApplications().withAnalytics().build();

        assertThat(report.applications()).isEmpty();
        assertThat(report.analytics().applicationCount()).isZero();
        PdfReader reader = new PdfReader(new PdfReportRenderer().render(report));
        try {
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(text).contains("No applications fall within this date range.", "Applied 1 Jan 2020 to 31 Dec 2020");
        } finally {
            reader.close();
        }
    }

    @Test
    void daysInStageAreMeasuredFromGenerationTimeInTheRequestedZone() {
        Report report = ReportBuilder.forOwner(fixture.owner(), fixture.applications())
                .generatedAt(fixture.generatedAt(), ZoneId.of("Asia/Kolkata"))
                .withApplications()
                .build();

        assertThat(report.zone()).isEqualTo(ZoneId.of("Asia/Kolkata"));
        assertThat(report.applications()).filteredOn(row -> row.companyName().equals("Hooli"))
                .singleElement().extracting(Report.ApplicationRow::daysInCurrentStage).isEqualTo(147L);
        assertThat(report.applications()).filteredOn(row -> row.status().isTerminal())
                .allSatisfy(row -> assertThat(row.daysInCurrentStage()).isNull());
    }

    @Test
    void buildRejectsAReportWithNoSectionsOrAnInvertedRange() {
        assertThatThrownBy(() -> builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least one section");
        assertThatThrownBy(() -> builder().withApplications().withDateRange(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1)).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("starts");
    }

    @Test
    void sectionsCanBeChosenFromAReportTypeAndTheTitleFollows() {
        assertThat(builder().withSectionsFor(ReportType.progress_report).build().title()).isEqualTo("Job search progress report");
        assertThat(builder().withSectionsFor(ReportType.applications).build().includesAnalytics()).isFalse();
        assertThat(builder().withSectionsFor(ReportType.analytics_summary).titled("For my mentor").build().title()).isEqualTo("For my mentor");
    }

    @Test
    void aCsvCannotHoldTwoSectionsAndNeutralisesFormulaCells() {
        Report both = builder().withApplications().withAnalytics().build();
        assertThatThrownBy(() -> csv.render(both)).isInstanceOf(IllegalArgumentException.class);

        assertThat(CsvReportRenderer.cell("=SUM(A1:A2)")).isEqualTo("'=SUM(A1:A2)");
        assertThat(CsvReportRenderer.cell("+44 20 7946 0000")).isEqualTo("'+44 20 7946 0000");
        assertThat(CsvReportRenderer.cell("@handle")).isEqualTo("'@handle");
        assertThat(CsvReportRenderer.cell("plain")).isEqualTo("plain");
        assertThat(CsvReportRenderer.cell("a \"quoted\", value")).isEqualTo("\"a \"\"quoted\"\", value\"");
    }

    private static String asText(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
