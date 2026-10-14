package com.cohort.api.service.report;

import com.cohort.api.dto.ConversionRateView;
import com.cohort.api.dto.DwellTimesResponse;
import com.cohort.api.dto.FunnelResponse;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.analytics.StageAttainment;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PdfReportRenderer implements ReportRenderer {
    private static final Color INK = new Color(0x1F, 0x29, 0x37);
    private static final Color MUTED = new Color(0x6B, 0x72, 0x80);
    private static final Color ACCENT = new Color(0x1D, 0x4E, 0xD8);
    private static final Color HEADER_FILL = new Color(0x1F, 0x29, 0x37);
    private static final Color ZEBRA_FILL = new Color(0xF3, 0xF4, 0xF6);
    private static final Color RULE = new Color(0xE5, 0xE7, 0xEB);
    private static final Color KPI_FILL = new Color(0xEF, 0xF6, 0xFF);

    private static final Font TITLE = font(Font.BOLD, 20, INK);
    private static final Font SUBTITLE = font(Font.NORMAL, 9.5f, MUTED);
    private static final Font SECTION = font(Font.BOLD, 13, ACCENT);
    private static final Font SUBSECTION = font(Font.BOLD, 10.5f, INK);
    private static final Font BODY = font(Font.NORMAL, 8.5f, INK);
    private static final Font BODY_MUTED = font(Font.ITALIC, 8.5f, MUTED);
    private static final Font TABLE_HEADER = font(Font.BOLD, 8.5f, Color.WHITE);
    private static final Font KPI_VALUE = font(Font.BOLD, 16, INK);
    private static final Font KPI_LABEL = font(Font.NORMAL, 8, MUTED);
    private static final Font NOTE = font(Font.NORMAL, 7.5f, MUTED);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    @Override
    public ReportFormat format() {
        return ReportFormat.pdf;
    }

    @Override
    public byte[] render(Report report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 40, 44);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new Footer(report.title()));
            document.addTitle(report.title());
            document.addAuthor("Cohort");
            document.addSubject("Prepared for " + report.ownerName());
            document.addCreator("Cohort job search tracker");
            document.open();

            writeTitleBlock(document, report);
            if (report.includesAnalytics()) {
                writeHeadlineFigures(document, report.analytics());
            }
            if (report.includesApplications()) {
                writeApplications(document, report);
            }
            if (report.includesAnalytics()) {
                writeAnalytics(document, report.analytics(), report.includesApplications());
            }
            document.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not render the PDF report", ex);
        }
        return out.toByteArray();
    }

    private void writeTitleBlock(Document document, Report report) throws DocumentException {
        Paragraph title = new Paragraph(report.title(), TITLE);
        title.setSpacingAfter(4);
        document.add(title);

        String subtitle = "Prepared for " + report.ownerName()
                + "   |   " + rangeDescription(report.dateFrom(), report.dateTo())
                + "   |   Generated " + DATE_TIME.format(report.generatedAt().atZone(report.zone())) + " (" + zoneLabel(report) + ")";
        Paragraph sub = new Paragraph(subtitle, SUBTITLE);
        sub.setSpacingAfter(14);
        document.add(sub);
    }

    private void writeHeadlineFigures(Document document, Report.AnalyticsSummary analytics) throws DocumentException {
        long active = analytics.statusBreakdown().entrySet().stream()
                .filter(entry -> !entry.getKey().isTerminal())
                .mapToLong(Map.Entry::getValue)
                .sum();
        FunnelResponse funnel = analytics.funnel();
        PdfPTable kpis = new PdfPTable(4);
        kpis.setWidthPercentage(100);
        kpis.setSpacingAfter(16);
        kpis.addCell(kpi(String.valueOf(analytics.applicationCount()), "Applications in this report"));
        kpis.addCell(kpi(String.valueOf(active), "Still active"));
        kpis.addCell(kpi(String.valueOf(funnel.reachedInterview()), "Reached an interview"));
        kpis.addCell(kpi(String.valueOf(funnel.reachedOffer()), "Reached an offer"));
        document.add(kpis);
    }

    private void writeApplications(Document document, Report report) throws DocumentException {
        document.add(sectionHeading("Applications (" + report.applications().size() + ")"));
        if (report.applications().isEmpty()) {
            document.add(new Paragraph("No applications fall within this date range.", BODY_MUTED));
            return;
        }
        String[] headers = {"Company", "Role", "Status", "Applied", "Days in stage", "Interviews", "Location", "Work mode", "Priority", "Source"};
        float[] widths = {15, 18, 11.5f, 8.5f, 6.5f, 7.5f, 10.5f, 7.5f, 6.5f, 8.5f};
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(18);
        for (String header : headers) {
            table.addCell(headerCell(header));
        }
        int index = 0;
        for (Report.ApplicationRow row : report.applications()) {
            Color fill = index++ % 2 == 1 ? ZEBRA_FILL : null;
            String company = row.companyName() + (row.referral() ? " (referral)" : "");
            table.addCell(bodyCell(company, fill, Element.ALIGN_LEFT));
            table.addCell(bodyCell(row.jobTitle(), fill, Element.ALIGN_LEFT));
            table.addCell(bodyCell(statusText(row), fill, Element.ALIGN_LEFT));
            table.addCell(bodyCell(DATE.format(row.dateApplied()), fill, Element.ALIGN_LEFT));
            table.addCell(bodyCell(row.daysInCurrentStage() == null ? "-" : String.valueOf(row.daysInCurrentStage()), fill, Element.ALIGN_RIGHT));
            table.addCell(bodyCell(String.valueOf(row.interviewCount()), fill, Element.ALIGN_RIGHT));
            table.addCell(bodyCell(row.location(), fill, Element.ALIGN_LEFT));
            table.addCell(bodyCell(ReportLabels.capitalised(row.workMode()), fill, Element.ALIGN_LEFT));
            table.addCell(bodyCell(ReportLabels.capitalised(row.priority()), fill, Element.ALIGN_LEFT));
            table.addCell(bodyCell(row.source(), fill, Element.ALIGN_LEFT));
        }
        document.add(table);
    }

    private void writeAnalytics(Document document, Report.AnalyticsSummary analytics, boolean startOnNewPage) throws DocumentException {
        if (startOnNewPage) {
            document.newPage();
        }
        document.add(sectionHeading("Analytics summary"));
        document.add(new Paragraph("Computed over the " + analytics.applicationCount()
                + " application(s) in this report, with the same formulas and sample-size rules as the Analytics page in Cohort.", BODY_MUTED));

        PdfPTable layout = new PdfPTable(new float[] {1, 1});
        layout.setWidthPercentage(100);
        layout.setSpacingBefore(10);
        layout.addCell(container(funnelTable(analytics.funnel())));
        layout.addCell(container(conversionTable(analytics)));
        layout.addCell(container(dwellTable(analytics.dwellTimes())));
        layout.addCell(container(outcomesTable(analytics)));
        document.add(layout);

        Paragraph note = new Paragraph(
                "How to read this: funnel counts include every application that ever reached a stage, even if it later moved on or closed. "
                        + "A conversion rate is only shown once at least 5 applications have been resolved at that stage (moved on, or closed); "
                        + "below that it is marked \"insufficient data\" rather than showing a number that could mislead. "
                        + "Median days in stage use completed stays only; applications still sitting in a stage are counted separately. "
                        + "\"No response\" means still open with no status change for 60+ days, and is never counted as a rejection.",
                NOTE);
        note.setSpacingBefore(10);
        document.add(note);
    }

    private PdfPTable funnelTable(FunnelResponse funnel) {
        PdfPTable table = miniTable("Pipeline funnel (ever reached)", new String[] {"Stage", "Applications", "Share of applied"}, new float[] {3, 2, 2});
        long base = funnel.applied();
        addFunnelRow(table, "Applied", funnel.applied(), base);
        addFunnelRow(table, "Online assessment", funnel.reachedOa(), base);
        addFunnelRow(table, "Interview", funnel.reachedInterview(), base);
        addFunnelRow(table, "Final round", funnel.reachedFinal(), base);
        addFunnelRow(table, "Offer", funnel.reachedOffer(), base);
        return table;
    }

    private void addFunnelRow(PdfPTable table, String label, long count, long base) {
        table.addCell(bodyCell(label, null, Element.ALIGN_LEFT));
        table.addCell(bodyCell(String.valueOf(count), null, Element.ALIGN_RIGHT));
        String share = base == 0 ? "-" : String.format(Locale.ENGLISH, "%.0f%%", 100.0 * count / base);
        table.addCell(bodyCell(share, null, Element.ALIGN_RIGHT));
    }

    private PdfPTable conversionTable(Report.AnalyticsSummary analytics) {
        PdfPTable table = miniTable("Conversion rates", new String[] {"Transition", "Rate", "Based on"}, new float[] {4, 2, 2.2f});
        for (Map.Entry<String, ConversionRateView> entry : ReportLabels.conversions(analytics.conversions()).entrySet()) {
            ConversionRateView rate = entry.getValue();
            table.addCell(bodyCell(entry.getKey(), null, Element.ALIGN_LEFT));
            table.addCell(rate.sufficientData()
                    ? bodyCell(rate.formatted(), null, Element.ALIGN_RIGHT)
                    : cell("insufficient data", BODY_MUTED, null, Element.ALIGN_RIGHT));
            table.addCell(bodyCell(rate.numerator() + " of " + rate.denominator(), null, Element.ALIGN_RIGHT));
        }
        return table;
    }

    private PdfPTable dwellTable(DwellTimesResponse dwellTimes) {
        PdfPTable table = miniTable("Median days in each stage", new String[] {"Stage", "Median", "Middle 50%", "Completed", "Still there"},
                new float[] {2.6f, 2.4f, 2.1f, 1.6f, 1.6f});
        for (ApplicationStatus stage : StageAttainment.pipelineStages()) {
            DwellTimesResponse.DwellView dwell = dwellTimes.byStage().get(stage);
            if (dwell == null) {
                continue;
            }
            table.addCell(bodyCell(ReportLabels.status(stage), null, Element.ALIGN_LEFT));
            if (dwell.sufficientData()) {
                table.addCell(bodyCell(ReportLabels.days(dwell.medianDays()) + " d", null, Element.ALIGN_RIGHT));
                table.addCell(bodyCell(ReportLabels.days(dwell.p25Days()) + " - " + ReportLabels.days(dwell.p75Days()) + " d", null, Element.ALIGN_RIGHT));
            } else {
                table.addCell(cell("insufficient data", BODY_MUTED, null, Element.ALIGN_RIGHT));
                table.addCell(bodyCell("-", null, Element.ALIGN_RIGHT));
            }
            table.addCell(bodyCell(String.valueOf(dwell.completedCount()), null, Element.ALIGN_RIGHT));
            table.addCell(bodyCell(String.valueOf(dwell.censoredCount()), null, Element.ALIGN_RIGHT));
        }
        return table;
    }

    private PdfPTable outcomesTable(Report.AnalyticsSummary analytics) {
        PdfPTable table = miniTable("Current status and outcomes", new String[] {"", "Applications"}, new float[] {4, 2});
        if (analytics.statusBreakdown().isEmpty()) {
            PdfPCell empty = cell("No applications in this range.", BODY_MUTED, null, Element.ALIGN_LEFT);
            empty.setColspan(2);
            table.addCell(empty);
        }
        for (Map.Entry<ApplicationStatus, Long> entry : analytics.statusBreakdown().entrySet()) {
            table.addCell(bodyCell("Status now: " + ReportLabels.status(entry.getKey()), null, Element.ALIGN_LEFT));
            table.addCell(bodyCell(String.valueOf(entry.getValue()), null, Element.ALIGN_RIGHT));
        }
        for (Map.Entry<ApplicationStatus, Long> entry : analytics.rejections().byStage().entrySet()) {
            table.addCell(bodyCell("Rejected at stage: " + ReportLabels.status(entry.getKey()), null, Element.ALIGN_LEFT));
            table.addCell(bodyCell(String.valueOf(entry.getValue()), null, Element.ALIGN_RIGHT));
        }
        table.addCell(bodyCell("No response (60+ days, still open)", null, Element.ALIGN_LEFT));
        table.addCell(bodyCell(String.valueOf(analytics.rejections().noResponseCount()), null, Element.ALIGN_RIGHT));
        return table;
    }

    private static PdfPTable miniTable(String title, String[] headers, float[] widths) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        PdfPCell caption = new PdfPCell(new Phrase(title, SUBSECTION));
        caption.setColspan(headers.length);
        caption.setBorder(Rectangle.NO_BORDER);
        caption.setPaddingBottom(6);
        caption.setPaddingLeft(0);
        table.addCell(caption);
        for (String header : headers) {
            table.addCell(headerCell(header));
        }
        table.setHeaderRows(2);
        return table;
    }

    private static PdfPCell container(PdfPTable inner) {
        PdfPCell cell = new PdfPCell(inner);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(6);
        cell.setPaddingBottom(12);
        return cell;
    }

    private static PdfPCell kpi(String value, String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(KPI_FILL);
        cell.setBorderColor(Color.WHITE);
        cell.setBorderWidth(3);
        cell.setPadding(9);
        Paragraph number = new Paragraph(value, KPI_VALUE);
        cell.addElement(number);
        cell.addElement(new Paragraph(label, KPI_LABEL));
        return cell;
    }

    private static Paragraph sectionHeading(String text) {
        Paragraph heading = new Paragraph(text, SECTION);
        heading.setSpacingBefore(4);
        heading.setSpacingAfter(8);
        return heading;
    }

    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER));
        cell.setBackgroundColor(HEADER_FILL);
        cell.setBorderColor(HEADER_FILL);
        cell.setPadding(5);
        return cell;
    }

    private static PdfPCell bodyCell(String text, Color fill, int alignment) {
        return cell(text == null || text.isBlank() ? "-" : text, BODY, fill, alignment);
    }

    private static PdfPCell cell(String text, Font font, Color fill, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4.5f);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(RULE);
        if (fill != null) {
            cell.setBackgroundColor(fill);
        }
        return cell;
    }

    private static String statusText(Report.ApplicationRow row) {
        String status = ReportLabels.status(row.status());
        if (row.status().isTerminal() && row.currentStage() != null && row.currentStage() != ApplicationStatus.SAVED) {
            return status + " (after " + ReportLabels.status(row.currentStage()).toLowerCase(Locale.ENGLISH) + ")";
        }
        return status;
    }

    private static String rangeDescription(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return "All applications";
        }
        if (from == null) {
            return "Applied up to " + DATE.format(to);
        }
        if (to == null) {
            return "Applied since " + DATE.format(from);
        }
        return "Applied " + DATE.format(from) + " to " + DATE.format(to);
    }

    private static String zoneLabel(Report report) {
        String id = report.zone().getId();
        return "Z".equals(id) ? "UTC" : id;
    }

    private static Font font(int style, float size, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    private static final class Footer extends PdfPageEventHelper {
        private final String title;

        Footer(String title) {
            this.title = title;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            float y = document.bottom() - 22;
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(new Chunk("Cohort  |  " + title, NOTE)), document.left(), y, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, new Phrase(new Chunk("Page " + writer.getPageNumber(), NOTE)), document.right(), y, 0);
        }
    }
}
