package com.cohort.api.service.calendar;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class IcsCalendar {
    private static final DateTimeFormatter UTC_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_LINE_OCTETS = 75;

    private final Instant stamp;
    private final List<String> lines = new ArrayList<>();

    public IcsCalendar(Instant stamp) {
        this.stamp = stamp;
        lines.add("BEGIN:VCALENDAR");
        lines.add("VERSION:2.0");
        lines.add("PRODID:-//Cohort//Interview calendar//EN");
        lines.add("CALSCALE:GREGORIAN");
        lines.add("METHOD:PUBLISH");
    }

    public IcsCalendar timedEvent(
            String uid, String summary, Instant start, Instant end, String location, String description, Integer reminderMinutes) {
        lines.add("BEGIN:VEVENT");
        lines.add("UID:" + uid);
        lines.add("DTSTAMP:" + UTC_STAMP.format(stamp));
        lines.add("DTSTART:" + UTC_STAMP.format(start));
        lines.add("DTEND:" + UTC_STAMP.format(end));
        addText("SUMMARY", summary);
        addOptionalText("LOCATION", location);
        addOptionalText("DESCRIPTION", description);
        if (reminderMinutes != null) {
            addAlarm(reminderMinutes);
        }
        lines.add("END:VEVENT");
        return this;
    }

    public IcsCalendar allDayEvent(String uid, String summary, LocalDate day, String description) {
        lines.add("BEGIN:VEVENT");
        lines.add("UID:" + uid);
        lines.add("DTSTAMP:" + UTC_STAMP.format(stamp));
        lines.add("DTSTART;VALUE=DATE:" + DATE_ONLY.format(day));
        lines.add("DTEND;VALUE=DATE:" + DATE_ONLY.format(day.plusDays(1)));
        addText("SUMMARY", summary);
        addOptionalText("DESCRIPTION", description);
        lines.add("END:VEVENT");
        return this;
    }

    public String render() {
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            out.append(fold(line));
        }
        out.append("END:VCALENDAR\r\n");
        return out.toString();
    }

    public static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }

    private void addAlarm(int minutesBefore) {
        lines.add("BEGIN:VALARM");
        lines.add("ACTION:DISPLAY");
        lines.add("DESCRIPTION:Reminder");
        lines.add("TRIGGER:-PT" + minutesBefore + "M");
        lines.add("END:VALARM");
    }

    private void addText(String name, String value) {
        lines.add(name + ":" + escape(value));
    }

    private void addOptionalText(String name, String value) {
        if (value != null && !value.isBlank()) {
            addText(name, value);
        }
    }

    // Lines are limited to 75 octets; continuation lines start with a space and must not split a UTF-8 character.
    private static String fold(String line) {
        StringBuilder out = new StringBuilder();
        int octets = 0;
        int limit = MAX_LINE_OCTETS;
        for (int i = 0; i < line.length(); ) {
            int codePoint = line.codePointAt(i);
            String ch = new String(Character.toChars(codePoint));
            int size = ch.getBytes(StandardCharsets.UTF_8).length;
            if (octets + size > limit) {
                out.append("\r\n ");
                octets = 0;
                limit = MAX_LINE_OCTETS - 1;
            }
            out.append(ch);
            octets += size;
            i += Character.charCount(codePoint);
        }
        return out.append("\r\n").toString();
    }
}
