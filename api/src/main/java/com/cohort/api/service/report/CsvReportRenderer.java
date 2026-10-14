package com.cohort.api.service.report;

import com.cohort.api.dto.ConversionRateView;
import com.cohort.api.dto.DwellTimesResponse;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.analytics.StageAttainment;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CsvReportRenderer implements ReportRenderer {
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String YES = "yes";
    private static final String NO = "no";

    @Override
    public ReportFormat format() {
        return ReportFormat.csv;
    }

    @Override
    public byte[] render(Report report) {
        if (report.includesApplications() == report.includesAnalytics()) {
            throw new IllegalArgumentException("A CSV export holds one table: applications or analytics.");
        }
        List<List<String>> rows = report.includesApplications() ? applicationRows(report) : analyticsRows(report);
        StringBuilder csv = new StringBuilder();
        for (List<String> row : rows) {
            csv.append(String.join(",", row.stream().map(CsvReportRenderer::cell).toList())).append("\r\n");
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, withBom, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, withBom, UTF8_BOM.length, body.length);
        return withBom;
    }

    private List<List<String>> applicationRows(Report report) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Company", "Role", "Status", "Current stage", "Date applied", "Days in current stage",
                "Interviews", "Location", "Work mode", "Priority", "Source", "Referral"));
        for (Report.ApplicationRow row : report.applications()) {
            rows.add(List.of(
                    nullToEmpty(row.companyName()),
                    nullToEmpty(row.jobTitle()),
                    ReportLabels.status(row.status()),
                    ReportLabels.status(row.currentStage()),
                    row.dateApplied().toString(),
                    row.daysInCurrentStage() == null ? "" : String.valueOf(row.daysInCurrentStage()),
                    String.valueOf(row.interviewCount()),
                    nullToEmpty(row.location()),
                    ReportLabels.capitalised(row.workMode()),
                    ReportLabels.capitalised(row.priority()),
                    nullToEmpty(row.source()),
                    row.referral() ? YES : NO));
        }
        return rows;
    }

    private List<List<String>> analyticsRows(Report report) {
        Report.AnalyticsSummary analytics = report.analytics();
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Section", "Metric", "Value", "Numerator", "Denominator", "Sample size", "Sufficient data", "Notes"));

        rows.add(List.of("Summary", "Applications in range", String.valueOf(analytics.applicationCount()), "", "",
                String.valueOf(analytics.applicationCount()), "", dateRangeNote(report)));
        for (Map.Entry<ApplicationStatus, Long> entry : analytics.statusBreakdown().entrySet()) {
            rows.add(List.of("Current status", ReportLabels.status(entry.getKey()), String.valueOf(entry.getValue()), "", "", "", "", ""));
        }

        var funnel = analytics.funnel();
        String funnelSample = String.valueOf(funnel.sampleSize());
        rows.add(List.of("Funnel (ever reached)", "Applied", String.valueOf(funnel.applied()), "", "", funnelSample, "", ""));
        rows.add(List.of("Funnel (ever reached)", "Online assessment", String.valueOf(funnel.reachedOa()), "", "", funnelSample, "", ""));
        rows.add(List.of("Funnel (ever reached)", "Interview", String.valueOf(funnel.reachedInterview()), "", "", funnelSample, "", ""));
        rows.add(List.of("Funnel (ever reached)", "Final round", String.valueOf(funnel.reachedFinal()), "", "", funnelSample, "", ""));
        rows.add(List.of("Funnel (ever reached)", "Offer", String.valueOf(funnel.reachedOffer()), "", "", funnelSample, "", ""));

        for (Map.Entry<String, ConversionRateView> entry : ReportLabels.conversions(analytics.conversions()).entrySet()) {
            ConversionRateView rate = entry.getValue();
            rows.add(List.of("Conversion rate", entry.getKey(), rate.formatted(), String.valueOf(rate.numerator()),
                    String.valueOf(rate.denominator()), String.valueOf(rate.denominator()), rate.sufficientData() ? YES : NO,
                    rate.sufficientData() ? "" : "Fewer than 5 resolved applications; rate withheld"));
        }

        for (ApplicationStatus stage : StageAttainment.pipelineStages()) {
            DwellTimesResponse.DwellView dwell = analytics.dwellTimes().byStage().get(stage);
            if (dwell == null) {
                continue;
            }
            rows.add(List.of("Median days in stage", ReportLabels.status(stage),
                    dwell.sufficientData() ? ReportLabels.days(dwell.medianDays()) : "insufficient data", "", "",
                    String.valueOf(dwell.completedCount()), dwell.sufficientData() ? YES : NO,
                    dwell.censoredCount() > 0 ? dwell.censoredCount() + " still in this stage (excluded)" : ""));
        }

        var rejections = analytics.rejections();
        for (Map.Entry<ApplicationStatus, Long> entry : rejections.byStage().entrySet()) {
            rows.add(List.of("Rejected at stage", ReportLabels.status(entry.getKey()), String.valueOf(entry.getValue()), "", "",
                    String.valueOf(rejections.sampleSize()), rejections.sufficientData() ? YES : NO, ""));
        }
        rows.add(List.of("No response", "Open with no activity for 60+ days", String.valueOf(rejections.noResponseCount()),
                "", "", "", "", "Counted separately from rejections"));
        return rows;
    }

    private static String dateRangeNote(Report report) {
        if (report.dateFrom() == null && report.dateTo() == null) {
            return "All dates";
        }
        return "Applied " + (report.dateFrom() == null ? "any time" : report.dateFrom().toString())
                + " to " + (report.dateTo() == null ? "today" : report.dateTo().toString());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    static String cell(String raw) {
        String value = raw == null ? "" : raw;
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        return needsQuoting ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
