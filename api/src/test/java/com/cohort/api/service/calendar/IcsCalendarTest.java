package com.cohort.api.service.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class IcsCalendarTest {
    private static final Instant STAMP = Instant.parse("2026-10-15T08:00:00Z");

    @Test
    void escapesSpecialCharactersInText() {
        assertEquals("a\\, b\\; c\\\\ d\\ne", IcsCalendar.escape("a, b; c\\ d\ne"));
    }

    @Test
    void timedEventUsesUtcCrlfAndAlarm() {
        String ics = new IcsCalendar(STAMP)
                .timedEvent("interview-1@cohort", "Acme interview", Instant.parse("2026-11-02T09:30:00Z"),
                        Instant.parse("2026-11-02T10:30:00Z"), "Pune, India", "Round one", 30)
                .render();

        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
        assertTrue(ics.contains("UID:interview-1@cohort\r\n"));
        assertTrue(ics.contains("DTSTAMP:20261015T080000Z\r\n"));
        assertTrue(ics.contains("DTSTART:20261102T093000Z\r\n"));
        assertTrue(ics.contains("DTEND:20261102T103000Z\r\n"));
        assertTrue(ics.contains("LOCATION:Pune\\, India\r\n"));
        assertTrue(ics.contains("TRIGGER:-PT30M\r\n"));
        assertFalse(ics.replace("\r\n", "").contains("\n"));
    }

    @Test
    void allDayEventEndsOnTheNextDay() {
        String ics = new IcsCalendar(STAMP)
                .allDayEvent("deadline-1@cohort", "Deadline", LocalDate.of(2026, 12, 31), null)
                .render();

        assertTrue(ics.contains("DTSTART;VALUE=DATE:20261231\r\n"));
        assertTrue(ics.contains("DTEND;VALUE=DATE:20270101\r\n"));
    }

    @Test
    void longLinesAreFoldedAtSeventyFiveOctets() {
        String ics = new IcsCalendar(STAMP)
                .allDayEvent("deadline-2@cohort", "é".repeat(120), LocalDate.of(2026, 12, 1), null)
                .render();

        for (String line : ics.split("\r\n")) {
            assertTrue(line.getBytes(StandardCharsets.UTF_8).length <= 75, line);
        }
        assertTrue(ics.contains("\r\n "));
    }
}
