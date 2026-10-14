package com.cohort.api.service.application.imports;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.exception.InvalidImportException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ApplicationCsvParser {
    public static final int MAX_ROWS = 500;
    public static final int MAX_COMPANY_LENGTH = 200;
    public static final int MAX_ROLE_LENGTH = 300;

    private static final Map<String, String> FIELD_BY_HEADER = Map.ofEntries(
            Map.entry("company", "company"),
            Map.entry("companyname", "company"),
            Map.entry("employer", "company"),
            Map.entry("role", "role"),
            Map.entry("title", "role"),
            Map.entry("jobtitle", "role"),
            Map.entry("position", "role"),
            Map.entry("status", "status"),
            Map.entry("appliedon", "appliedDate"),
            Map.entry("applieddate", "appliedDate"),
            Map.entry("dateapplied", "appliedDate"),
            Map.entry("applied", "appliedDate"),
            Map.entry("date", "appliedDate"),
            Map.entry("location", "location"),
            Map.entry("url", "url"),
            Map.entry("joburl", "url"),
            Map.entry("link", "url"),
            Map.entry("notes", "notes"),
            Map.entry("note", "notes"));

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("uuuu-M-d", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/M/uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-M-uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH));

    private ApplicationCsvParser() {
    }

    public record Column(String header, String field) {
    }

    public record RawRow(int line, Map<String, String> values) {
    }

    public record Parsed(List<Column> columns, List<RawRow> rows) {
    }

    public record Candidate(
            int line,
            String company,
            String role,
            ApplicationStatus status,
            LocalDate dateApplied,
            String location,
            String url,
            String notes,
            List<String> warnings,
            String error) {
        public boolean valid() {
            return error == null;
        }
    }

    public static Parsed parse(byte[] content) {
        List<List<String>> records = tokenize(decode(content));
        if (records.isEmpty()) {
            throw new InvalidImportException("The file is empty.");
        }
        List<String> header = records.get(0);
        List<Column> columns = header.stream().map(ApplicationCsvParser::toColumn).toList();
        boolean hasCompany = columns.stream().anyMatch(column -> "company".equals(column.field()));
        boolean hasRole = columns.stream().anyMatch(column -> "role".equals(column.field()));
        if (!hasCompany || !hasRole) {
            throw new InvalidImportException("The file needs a company column and a role or title column.");
        }
        if (records.size() - 1 > MAX_ROWS) {
            throw new InvalidImportException("The file has more than " + MAX_ROWS + " rows.");
        }

        List<RawRow> rows = new ArrayList<>();
        for (int index = 1; index < records.size(); index++) {
            List<String> cells = records.get(index);
            Map<String, String> values = new java.util.HashMap<>();
            for (int column = 0; column < columns.size() && column < cells.size(); column++) {
                String field = columns.get(column).field();
                if (field != null && !values.containsKey(field)) {
                    values.put(field, cells.get(column).trim());
                }
            }
            rows.add(new RawRow(index + 1, values));
        }
        return new Parsed(columns, rows);
    }

    public static Candidate interpret(RawRow row, LocalDate today) {
        Map<String, String> values = row.values();
        String company = values.getOrDefault("company", "");
        String role = values.getOrDefault("role", "");
        List<String> warnings = new ArrayList<>();

        if (company.isEmpty() || role.isEmpty()) {
            return invalid(row, company, role, "Company and role are both required.");
        }
        if (company.length() > MAX_COMPANY_LENGTH || role.length() > MAX_ROLE_LENGTH) {
            return invalid(row, company, role, "Company or role is too long.");
        }

        ApplicationStatus status = parseStatus(values.getOrDefault("status", ""), warnings);
        LocalDate dateApplied = parseDate(values.getOrDefault("appliedDate", ""), today, warnings);
        return new Candidate(
                row.line(), company, role, status, dateApplied,
                blankToNull(values.get("location")), blankToNull(values.get("url")), blankToNull(values.get("notes")),
                warnings, null);
    }

    private static Candidate invalid(RawRow row, String company, String role, String error) {
        return new Candidate(row.line(), company, role, null, null, null, null, null, List.of(), error);
    }

    private static ApplicationStatus parseStatus(String raw, List<String> warnings) {
        if (raw.isEmpty()) {
            return ApplicationStatus.SAVED;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
        try {
            return ApplicationStatus.valueOf(normalized);
        } catch (IllegalArgumentException unknown) {
            warnings.add("Unknown status \"" + raw + "\", saved as Saved.");
            return ApplicationStatus.SAVED;
        }
    }

    private static LocalDate parseDate(String raw, LocalDate today, List<String> warnings) {
        if (raw.isEmpty()) {
            warnings.add("No applied date, used today.");
            return today;
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                LocalDate parsed = LocalDate.parse(raw, format);
                if (parsed.isAfter(today)) {
                    warnings.add("Applied date is in the future, used today.");
                    return today;
                }
                return parsed;
            } catch (DateTimeParseException tryNext) {
                continue;
            }
        }
        warnings.add("Could not read the date \"" + raw + "\", used today.");
        return today;
    }

    private static Column toColumn(String header) {
        String key = header.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return new Column(header.trim(), FIELD_BY_HEADER.get(key));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String decode(byte[] content) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
            return text.startsWith("﻿") ? text.substring(1) : text;
        } catch (CharacterCodingException notUtf8) {
            throw new InvalidImportException("The file is not valid UTF-8.");
        }
    }

    private static List<List<String>> tokenize(String text) {
        char delimiter = detectDelimiter(text);
        List<List<String>> records = new ArrayList<>();
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == delimiter) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                endRecord(records, cells, cell);
            } else {
                cell.append(c);
            }
        }
        endRecord(records, cells, cell);
        return records;
    }

    private static void endRecord(List<List<String>> records, List<String> cells, StringBuilder cell) {
        cells.add(cell.toString());
        cell.setLength(0);
        boolean blank = cells.stream().allMatch(String::isBlank);
        if (!blank) {
            records.add(new ArrayList<>(cells));
        }
        cells.clear();
    }

    private static char detectDelimiter(String text) {
        int end = text.indexOf('\n');
        String firstLine = end < 0 ? text : text.substring(0, end);
        char best = ',';
        long bestCount = firstLine.chars().filter(c -> c == ',').count();
        for (char candidate : new char[] {';', '\t'}) {
            long count = firstLine.chars().filter(c -> c == candidate).count();
            if (count > bestCount) {
                best = candidate;
                bestCount = count;
            }
        }
        return best;
    }
}
