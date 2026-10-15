package com.cohort.api.service.resume;

import com.cohort.api.entity.Resume;
import com.cohort.api.entity.ResumeSection;
import com.cohort.api.entity.ResumeSection.SectionType;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ResumePdfBuilder {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    private static final List<SectionType> ORDER = List.of(
            SectionType.experience, SectionType.project, SectionType.skill, SectionType.education, SectionType.achievement);

    private static final Font NAME = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font ROLE = new Font(Font.HELVETICA, 10.5f, Font.NORMAL);
    private static final Font HEADING = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font ENTRY = new Font(Font.HELVETICA, 9.5f, Font.BOLD);
    private static final Font BODY = new Font(Font.HELVETICA, 9, Font.NORMAL);

    public byte[] build(Resume resume, List<ResumeSection> sections) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 48, 48, 44, 44);
        try {
            PdfWriter.getInstance(document, out);
            document.addTitle(resume.getName());
            document.open();

            document.add(new Paragraph(resume.getName(), NAME));
            if (hasText(resume.getTargetRole())) {
                document.add(new Paragraph(resume.getTargetRole(), ROLE));
            }
            if (hasText(resume.getSummary())) {
                document.add(heading("Summary"));
                document.add(new Paragraph(resume.getSummary(), BODY));
            }
            for (SectionType type : ORDER) {
                List<ResumeSection> ofType = sections.stream().filter(s -> s.getSectionType() == type).toList();
                if (ofType.isEmpty()) {
                    continue;
                }
                document.add(heading(headingFor(type)));
                for (ResumeSection section : ofType) {
                    writeEntry(document, section);
                }
            }
            document.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not render the resume PDF", ex);
        }
        return out.toByteArray();
    }

    private void writeEntry(Document document, ResumeSection section) throws DocumentException {
        String headline = List.of(nullToEmpty(section.getTitle()), nullToEmpty(section.getOrganisation())).stream()
                .filter(this::hasText)
                .collect(Collectors.joining(", "));
        String dates = dateRange(section.getStartDate(), section.getEndDate());
        if (hasText(dates)) {
            headline = headline.isEmpty() ? dates : headline + "  (" + dates + ")";
        }
        if (hasText(headline)) {
            Paragraph line = new Paragraph(headline, ENTRY);
            line.setSpacingBefore(4);
            document.add(line);
        }
        if (hasText(section.getDescription())) {
            document.add(new Paragraph(section.getDescription(), BODY));
        }
    }

    private Paragraph heading(String text) {
        Paragraph heading = new Paragraph(text.toUpperCase(Locale.ENGLISH), HEADING);
        heading.setSpacingBefore(12);
        heading.setSpacingAfter(2);
        return heading;
    }

    private String headingFor(SectionType type) {
        return switch (type) {
            case experience -> "Experience";
            case project -> "Projects";
            case skill -> "Skills";
            case education -> "Education";
            case achievement -> "Achievements";
        };
    }

    private String dateRange(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "";
        }
        String from = start == null ? "" : MONTH.format(start);
        String to = end == null ? "Present" : MONTH.format(end);
        return from.isEmpty() ? to : from + " - " + to;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
