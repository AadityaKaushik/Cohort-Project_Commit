package com.cohort.api.service;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.Interview;
import com.cohort.api.exception.InterviewNotFoundException;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.InterviewRepository;
import com.cohort.api.repository.specification.InterviewSpecifications;
import com.cohort.api.service.calendar.IcsCalendar;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarService {
    static final int DEFAULT_DURATION_MINUTES = 60;
    static final int REMINDER_MINUTES = 30;

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final Clock clock;

    public CalendarService(
            InterviewRepository interviewRepository,
            ApplicationRepository applicationRepository,
            CompanyRepository companyRepository,
            Clock clock) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public String interviewCalendar(UUID userId, UUID interviewId) {
        Interview interview = interviewRepository
                .findByIdAndUserId(interviewId, userId)
                .orElseThrow(InterviewNotFoundException::new);
        if (interview.getScheduledAt() == null) {
            throw new InterviewNotFoundException();
        }
        IcsCalendar calendar = new IcsCalendar(clock.instant());
        addInterview(calendar, userId, interview);
        return calendar.render();
    }

    @Transactional(readOnly = true)
    public String upcomingCalendar(UUID userId) {
        IcsCalendar calendar = new IcsCalendar(clock.instant());
        List<Interview> interviews = interviewRepository.findAll(InterviewSpecifications.combine(userId, null, true, null));
        interviews.stream()
                .sorted((a, b) -> a.getScheduledAt().compareTo(b.getScheduledAt()))
                .forEach(interview -> addInterview(calendar, userId, interview));

        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        for (Application application : applicationRepository.findAllByUserId(userId)) {
            LocalDate deadline = application.getDeadline();
            if (deadline == null || deadline.isBefore(today) || application.getArchivedAt() != null
                    || application.getStatus().isTerminal()) {
                continue;
            }
            Company company = requireCompany(userId, application.getCompanyId());
            calendar.allDayEvent(
                    "deadline-" + application.getId() + "@cohort",
                    "Application deadline: " + company.getName() + ", " + application.getJobTitle(),
                    deadline,
                    application.getJobUrl());
        }
        return calendar.render();
    }

    private void addInterview(IcsCalendar calendar, UUID userId, Interview interview) {
        Application application = applicationRepository
                .findByIdAndUserId(interview.getApplicationId(), userId)
                .orElseThrow(InterviewNotFoundException::new);
        Company company = requireCompany(userId, application.getCompanyId());
        int minutes = interview.getDurationMinutes() != null ? interview.getDurationMinutes() : DEFAULT_DURATION_MINUTES;
        Instant start = interview.getScheduledAt();
        String summary = company.getName() + " interview, round " + interview.getRoundNumber()
                + " (" + roundLabel(interview) + ")";
        calendar.timedEvent(
                "interview-" + interview.getId() + "@cohort",
                summary,
                start,
                start.plus(Duration.ofMinutes(minutes)),
                application.getLocation() != null ? application.getLocation() : application.getJobUrl(),
                description(interview, application),
                REMINDER_MINUTES);
    }

    private String description(Interview interview, Application application) {
        StringBuilder text = new StringBuilder(application.getJobTitle());
        if (interview.getMode() != null) {
            text.append("\n").append("Mode: ").append(interview.getMode());
        }
        if (interview.getInterviewerName() != null) {
            text.append("\n").append("Interviewer: ").append(interview.getInterviewerName());
        }
        if (application.getJobUrl() != null) {
            text.append("\n").append(application.getJobUrl());
        }
        return text.toString();
    }

    private String roundLabel(Interview interview) {
        return interview.getRoundType().name().toLowerCase().replace('_', ' ');
    }

    private Company requireCompany(UUID userId, UUID companyId) {
        return companyRepository
                .findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> new IllegalStateException("Application references a company it does not own"));
    }
}
