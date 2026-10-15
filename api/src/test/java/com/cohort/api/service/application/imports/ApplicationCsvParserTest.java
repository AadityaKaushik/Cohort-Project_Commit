package com.cohort.api.service.application.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.exception.InvalidImportException;
import com.cohort.api.service.application.imports.ApplicationCsvParser.Candidate;
import com.cohort.api.service.application.imports.ApplicationCsvParser.Parsed;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ApplicationCsvParserTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    private static Parsed parse(String csv) {
        return ApplicationCsvParser.parse(csv.getBytes(StandardCharsets.UTF_8));
    }

    private static Candidate first(String csv) {
        return ApplicationCsvParser.interpret(parse(csv).rows().get(0), TODAY);
    }

    @Test
    void matchesHeadersIgnoringCaseSpacesAndPunctuation() {
        Parsed parsed = parse("  COMPANY ,Job Title,Applied Date,Job-URL\nAcme,Engineer,2026-01-05,https://acme.com\n");

        assertThat(parsed.columns()).extracting(ApplicationCsvParser.Column::field)
                .containsExactly("company", "role", "appliedDate", "url");
        Candidate row = ApplicationCsvParser.interpret(parsed.rows().get(0), TODAY);
        assertThat(row.company()).isEqualTo("Acme");
        assertThat(row.role()).isEqualTo("Engineer");
        assertThat(row.dateApplied()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(row.url()).isEqualTo("https://acme.com");
    }

    @Test
    void readsQuotedCellsWithCommasQuotesAndNewlines() {
        Candidate row = first("company,role,notes\r\n\"Acme, Inc\",Engineer,\"said \"\"hi\"\"\nthen left\"\r\n");

        assertThat(row.company()).isEqualTo("Acme, Inc");
        assertThat(row.notes()).isEqualTo("said \"hi\"\nthen left");
    }

    @Test
    void stripsBomAndAcceptsSemicolons() {
        Candidate row = first("﻿company;role;status\nAcme;Engineer;applied\n");

        assertThat(row.status()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void unknownStatusFallsBackToSavedWithWarning() {
        Candidate row = first("company,role,status\nAcme,Engineer,ghosted\n");

        assertThat(row.status()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(row.warnings()).anyMatch(warning -> warning.contains("ghosted"));
    }

    @Test
    void missingStatusIsSavedWithoutWarning() {
        Candidate row = first("company,role,applied date\nAcme,Engineer,05/01/2026\n");

        assertThat(row.status()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(row.dateApplied()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(row.warnings()).isEmpty();
    }

    @Test
    void badOrFutureDatesUseTodayWithWarning() {
        assertThat(first("company,role,applied date\nAcme,Engineer,soon\n").dateApplied()).isEqualTo(TODAY);
        assertThat(first("company,role,applied date\nAcme,Engineer,2030-01-01\n").warnings()).hasSize(1);
    }

    @Test
    void rowsMissingCompanyOrRoleAreInvalid() {
        Candidate row = first("company,role\n,Engineer\n");

        assertThat(row.valid()).isFalse();
    }

    @Test
    void rejectsMissingRequiredColumns() {
        assertThatThrownBy(() -> parse("company,status\nAcme,applied\n")).isInstanceOf(InvalidImportException.class);
    }

    @Test
    void rejectsMoreThanFiveHundredRows() {
        StringBuilder csv = new StringBuilder("company,role\n");
        for (int i = 0; i < 501; i++) {
            csv.append("Co").append(i).append(",Dev\n");
        }

        assertThatThrownBy(() -> parse(csv.toString())).isInstanceOf(InvalidImportException.class);
    }

    @Test
    void rejectsBytesThatAreNotUtf8() {
        byte[] latin1 = "company,role\nCafé,Dev\n".getBytes(StandardCharsets.ISO_8859_1);

        assertThatThrownBy(() -> ApplicationCsvParser.parse(latin1)).isInstanceOf(InvalidImportException.class);
    }

    @Test
    void skipsBlankLines() {
        assertThat(parse("company,role\n\nAcme,Dev\n,\n").rows()).hasSize(1);
    }

    private static final String LINKEDIN = "\uFEFFApplication Date,Contact Email,Contact Phone Number,Company Name,Job Title,Job Url,Resume Name,Question and Answers\r\n"
            + "10/03/25 09:41 PM,,,\"Acme, Inc\",Backend Engineer,https://www.linkedin.com/jobs/view/123,cv.pdf,\r\n"
            + "\r\n"
            + "2025-09-28T14:05:00Z,jo@example.com,,Globex,\"Data \"\"Platform\"\" Engineer\",https://www.linkedin.com/jobs/view/456,cv.pdf,\r\n"
            + "09/15/2025 08:02:11 AM,,,Initech,QA Analyst,,,\r\n";

    @Test
    void detectsLinkedInExportAndDefaultsToApplied() {
        Parsed parsed = parse(LINKEDIN);

        assertThat(parsed.source()).isEqualTo(ApplicationCsvParser.SOURCE_LINKEDIN);
        assertThat(parsed.rows()).hasSize(3);
        Candidate first = ApplicationCsvParser.interpret(parsed.rows().get(0), TODAY, parsed.source());
        assertThat(first.company()).isEqualTo("Acme, Inc");
        assertThat(first.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(first.dateApplied()).isEqualTo(LocalDate.of(2025, 10, 3));
        assertThat(first.url()).isEqualTo("https://www.linkedin.com/jobs/view/123");
        assertThat(first.warnings()).isEmpty();
    }

    @Test
    void readsLinkedInDateVariants() {
        Parsed parsed = parse(LINKEDIN);

        Candidate iso = ApplicationCsvParser.interpret(parsed.rows().get(1), TODAY, parsed.source());
        Candidate long4 = ApplicationCsvParser.interpret(parsed.rows().get(2), TODAY, parsed.source());
        assertThat(iso.role()).isEqualTo("Data \"Platform\" Engineer");
        assertThat(iso.dateApplied()).isEqualTo(LocalDate.of(2025, 9, 28));
        assertThat(long4.dateApplied()).isEqualTo(LocalDate.of(2025, 9, 15));
    }

    @Test
    void detectsTrackerLayoutAndKeepsSalaryInNotes() {
        Parsed parsed = parse("Company,Position,Status,Date Applied,Link,Location,Notes,Salary\n"
                + "Acme,Engineer,Interview,2026-02-01,https://acme.com/j,Pune,Referred,12 LPA\n"
                + "Globex,Analyst,Ghosted,,,,,\n");

        assertThat(parsed.source()).isEqualTo(ApplicationCsvParser.SOURCE_TRACKER);
        Candidate first = ApplicationCsvParser.interpret(parsed.rows().get(0), TODAY, parsed.source());
        assertThat(first.status()).isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(first.notes()).isEqualTo("Referred\nSalary: 12 LPA");
        assertThat(first.location()).isEqualTo("Pune");
        Candidate second = ApplicationCsvParser.interpret(parsed.rows().get(1), TODAY, parsed.source());
        assertThat(second.status()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(second.warnings()).anyMatch(warning -> warning.contains("Ghosted"));
    }

    @Test
    void plainFormatIsStandard() {
        assertThat(parse("company,role\nAcme,Dev\n").source()).isEqualTo(ApplicationCsvParser.SOURCE_STANDARD);
    }
}
