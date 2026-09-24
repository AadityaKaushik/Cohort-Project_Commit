package com.cohort.api.service.detection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewFeedback;
import com.cohort.api.entity.InterviewRoundType;
import com.cohort.api.event.MissingDebriefEvent;
import com.cohort.api.event.UpcomingInterviewEvent;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.InterviewFeedbackRepository;
import com.cohort.api.repository.InterviewRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * P59's verify (Observer) for the two interview-driven events: each fires on
 * exactly its FR-99 condition -- due within 24 hours (US-77), or past without
 * a submitted debrief (US-78) -- and on nothing else, published into a bus
 * with no subscribers.
 */
class InterviewEventDetectorTest {

    private static final Instant NOW = Instant.parse("2026-09-25T12:00:00Z");

    private final InterviewRepository interviewRepository = mock(InterviewRepository.class);
    private final InterviewFeedbackRepository feedbackRepository = mock(InterviewFeedbackRepository.class);
    private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final RecordingEventPublisher publisher = new RecordingEventPublisher();
    private final InterviewEventDetector detector = new InterviewEventDetector(
            interviewRepository, feedbackRepository, applicationRepository, companyRepository, publisher);

    private final UUID userId = UUID.randomUUID();
    private final List<Interview> interviews = new ArrayList<>();
    private final List<InterviewFeedback> feedback = new ArrayList<>();
    private Application application;

    @BeforeEach
    void setUp() {
        Company company = withId(new Company("Acme", "acme", userId));
        application = withId(new Application(userId, company.getId(), "Backend Engineer", LocalDate.of(2026, 9, 1), ApplicationStatus.INTERVIEW));
        when(interviewRepository.findAllByUserId(userId)).thenReturn(interviews);
        when(applicationRepository.findAllByUserId(userId)).thenAnswer(invocation -> List.of(application));
        when(companyRepository.findAllByUserId(userId)).thenReturn(List.of(company));
        when(feedbackRepository.findAllByUserIdAndInterviewIdIn(eq(userId), any())).thenReturn(feedback);
    }

    @Test
    void anInterviewDueWithin24HoursPublishesAnUpcomingInterviewEvent() {
        Interview interview = interviewAt(NOW.plus(Duration.ofHours(3)));

        InterviewEventDetector.DetectionSummary summary = detector.detect(userId, NOW);

        assertThat(summary).isEqualTo(new InterviewEventDetector.DetectionSummary(1, 0));
        assertThat(publisher.ofType(UpcomingInterviewEvent.class)).containsExactly(new UpcomingInterviewEvent(
                userId, interview.getId(), application.getId(), "Acme", "Backend Engineer",
                (short) 1, InterviewRoundType.TECHNICAL, interview.getScheduledAt()));
    }

    @Test
    void anInterviewExactlyAtTheEdgeOfTheWindowCountsButOneBeyondItDoesNot() {
        interviewAt(NOW.plus(Duration.ofHours(24)));
        interviewAt(NOW.plus(Duration.ofHours(24)).plusSeconds(1));

        assertThat(detector.detect(userId, NOW).upcomingInterviews()).isEqualTo(1);
    }

    @Test
    void noReminderForAnInterviewOnAWithdrawnApplicationOrOneWithAnOutcomeAlready() {
        interviewAt(NOW.plus(Duration.ofHours(2))).setOutcome(Interview.Outcome.passed);
        assertThat(detector.detect(userId, NOW).upcomingInterviews()).isZero();

        interviews.clear();
        interviewAt(NOW.plus(Duration.ofHours(2)));
        application.applyStatusChange(ApplicationStatus.WITHDRAWN);
        assertThat(detector.detect(userId, NOW).upcomingInterviews()).isZero();
    }

    @Test
    void aPastInterviewWithNoDebriefPublishesAMissingDebriefEvent() {
        Interview interview = interviewAt(NOW.minus(Duration.ofHours(5)));

        detector.detect(userId, NOW);

        assertThat(publisher.ofType(MissingDebriefEvent.class)).containsExactly(new MissingDebriefEvent(
                userId, interview.getId(), application.getId(), "Acme", "Backend Engineer",
                (short) 1, InterviewRoundType.TECHNICAL, interview.getScheduledAt(), false));
        assertThat(publisher.ofType(UpcomingInterviewEvent.class)).isEmpty();
    }

    @Test
    void aSavedDraftStillCountsAsMissingButIsFlaggedAsStarted() {
        Interview interview = interviewAt(NOW.minus(Duration.ofHours(5)));
        feedback.add(new InterviewFeedback(interview.getId(), userId));

        detector.detect(userId, NOW);

        assertThat(publisher.ofType(MissingDebriefEvent.class)).singleElement()
                .extracting(MissingDebriefEvent::draftStarted).isEqualTo(true);
    }

    @Test
    void aSubmittedDebriefSuppressesTheEvent() {
        Interview interview = interviewAt(NOW.minus(Duration.ofHours(5)));
        InterviewFeedback submitted = new InterviewFeedback(interview.getId(), userId);
        submitted.setDraft(false);
        feedback.add(submitted);

        assertThat(detector.detect(userId, NOW).missingDebriefs()).isZero();
    }

    @Test
    void noNudgeWhileTheInterviewIsStillWithinItsGracePeriod() {
        // 60-minute interview that started 2.5h ago ended 1.5h ago -- inside the 2h grace window.
        interviewAt(NOW.minus(Duration.ofMinutes(150))).setDurationMinutes(60);
        assertThat(detector.detect(userId, NOW).missingDebriefs()).isZero();

        interviews.clear();
        // Exactly 2h after a 60-minute interview ended: now due.
        interviewAt(NOW.minus(Duration.ofHours(3))).setDurationMinutes(60);
        assertThat(detector.detect(userId, NOW).missingDebriefs()).isEqualTo(1);
    }

    @Test
    void interviewsOlderThanTheLookbackAreNotNudgedAbout() {
        interviewAt(NOW.minus(Duration.ofDays(20)));

        assertThat(detector.detect(userId, NOW).missingDebriefs()).isZero();
        assertThat(publisher.published).isEmpty();
    }

    @Test
    void unscheduledInterviewsAreIgnoredEntirely() {
        Interview unscheduled = withId(new Interview(application.getId(), userId, (short) 1, InterviewRoundType.HR));
        interviews.add(unscheduled);

        assertThat(detector.detect(userId, NOW)).isEqualTo(new InterviewEventDetector.DetectionSummary(0, 0));
        assertThat(publisher.published).isEmpty();
    }

    @Test
    void theDetectorDependsOnNoSubscriberOnlyOnTheGenericPublisher() {
        DetectorIndependenceAssertions.assertKnowsNoSubscriber(InterviewEventDetector.class);
    }

    private Interview interviewAt(Instant scheduledAt) {
        Interview interview = withId(new Interview(application.getId(), userId, (short) (interviews.size() + 1), InterviewRoundType.TECHNICAL));
        interview.setScheduledAt(scheduledAt);
        interviews.add(interview);
        return interview;
    }

    private static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }
}
