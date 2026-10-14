package com.cohort.api.service;

import com.cohort.api.dto.ActivityDay;
import com.cohort.api.dto.ActivityResponse;
import com.cohort.api.dto.GoalResponse;
import com.cohort.api.dto.SetGoalRequest;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.Contact;
import com.cohort.api.entity.Goal;
import com.cohort.api.entity.Interview;
import com.cohort.api.entity.PracticeAttempt;
import com.cohort.api.exception.GoalNotFoundException;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.ContactRepository;
import com.cohort.api.repository.GoalRepository;
import com.cohort.api.repository.InterviewRepository;
import com.cohort.api.repository.PracticeAttemptRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {

    private static final int MAX_WEEKS = 52;

    private final GoalRepository goalRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final PracticeAttemptRepository practiceAttemptRepository;
    private final ContactRepository contactRepository;

    public GoalService(
            GoalRepository goalRepository,
            ApplicationRepository applicationRepository,
            InterviewRepository interviewRepository,
            PracticeAttemptRepository practiceAttemptRepository,
            ContactRepository contactRepository) {
        this.goalRepository = goalRepository;
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
        this.practiceAttemptRepository = practiceAttemptRepository;
        this.contactRepository = contactRepository;
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(UUID userId) {
        Map<Goal.Metric, List<LocalDate>> events = eventDates(userId);
        LocalDate weekStart = LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return goalRepository.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing(Goal::getMetric))
                .map(goal -> toResponse(goal, events.get(goal.getMetric()), weekStart))
                .toList();
    }

    @Transactional
    public GoalResponse set(UUID userId, SetGoalRequest request) {
        Goal goal = goalRepository.findByUserIdAndMetric(userId, request.metric())
                .orElseGet(() -> new Goal(userId, request.metric(), request.weeklyTarget()));
        goal.setWeeklyTarget(request.weeklyTarget());
        goalRepository.save(goal);
        LocalDate weekStart = LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return toResponse(goal, eventDates(userId).get(goal.getMetric()), weekStart);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        goalRepository.findByIdAndUserId(id, userId).orElseThrow(GoalNotFoundException::new);
        goalRepository.deleteByIdAndUserId(id, userId);
    }

    @Transactional(readOnly = true)
    public ActivityResponse activity(UUID userId, int weeks) {
        int span = Math.max(1, Math.min(weeks, MAX_WEEKS));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate first = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(span - 1L);
        Map<LocalDate, Long> perDay = eventDates(userId).values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(date -> date, Collectors.counting()));

        List<ActivityDay> days = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(today); day = day.plusDays(1)) {
            days.add(new ActivityDay(day, perDay.getOrDefault(day, 0L).intValue()));
        }
        return new ActivityResponse(days, currentStreak(perDay, today), longestStreak(perDay));
    }

    private GoalResponse toResponse(Goal goal, List<LocalDate> dates, LocalDate weekStart) {
        int progress = (int) dates.stream().filter(date -> !date.isBefore(weekStart)).count();
        return new GoalResponse(goal.getId(), goal.getMetric(), goal.getWeeklyTarget(), progress,
                progress >= goal.getWeeklyTarget());
    }

    private int currentStreak(Map<LocalDate, Long> perDay, LocalDate today) {
        LocalDate cursor = perDay.containsKey(today) ? today : today.minusDays(1);
        int streak = 0;
        while (perDay.containsKey(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int longestStreak(Map<LocalDate, Long> perDay) {
        List<LocalDate> sorted = perDay.keySet().stream().sorted().toList();
        int longest = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate date : sorted) {
            run = previous != null && previous.plusDays(1).equals(date) ? run + 1 : 1;
            longest = Math.max(longest, run);
            previous = date;
        }
        return longest;
    }

    private Map<Goal.Metric, List<LocalDate>> eventDates(UUID userId) {
        Instant now = Instant.now();
        Map<Goal.Metric, List<LocalDate>> events = new EnumMap<>(Goal.Metric.class);
        events.put(Goal.Metric.applications, applicationRepository.findAllByUserId(userId).stream()
                .map(Application::getDateApplied).filter(Objects::nonNull).toList());
        events.put(Goal.Metric.interviews, interviewRepository.findAllByUserId(userId).stream()
                .map(Interview::getScheduledAt).filter(at -> at != null && !at.isAfter(now)).map(GoalService::day).toList());
        events.put(Goal.Metric.practice_attempts, practiceAttemptRepository.findAllByUserId(userId).stream()
                .map(PracticeAttempt::getAttemptedAt).filter(Objects::nonNull).map(GoalService::day).toList());
        events.put(Goal.Metric.contacts_reached, contactRepository.findAllByUserId(userId).stream()
                .map(Contact::getLastContactedAt).filter(Objects::nonNull).map(GoalService::day).toList());
        return events;
    }

    private static LocalDate day(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
