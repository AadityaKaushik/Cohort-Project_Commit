package com.cohort.api.controller;

import com.cohort.api.service.CalendarService;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {
    private static final MediaType CALENDAR = MediaType.parseMediaType("text/calendar;charset=UTF-8");

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/upcoming.ics")
    public ResponseEntity<String> upcoming(@AuthenticationPrincipal Jwt jwt) {
        return icsResponse(calendarService.upcomingCalendar(UUID.fromString(jwt.getSubject())), "cohort-upcoming.ics");
    }

    static ResponseEntity<String> icsResponse(String body, String filename) {
        return ResponseEntity.ok()
                .contentType(CALENDAR)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
