package com.cohort.api.service.detection;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewFeedback;
import com.cohort.api.event.MissingDebriefEvent;
import com.cohort.api.event.UpcomingInterviewEvent;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.InterviewFeedbackRepository;
import com.cohort.api.repository.InterviewRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Observer pattern, subject side (ARCHITECTURE_REVISION_JAVA.md §4.1, FR-99):
 * scans one user's interviews and publishes an {@link UpcomingInterviewEvent}
 * (US-77) for every interview due within the next 24 hours, and a
 * {@link MissingDebriefEvent} (US-78) for every recent interview whose date
 * has passed without a submitted debrief. Like
 * {@link StalledApplicationDetector}, it depends only on repositories and
 * Spring's generic {@link ApplicationEventPublisher} -- it has no idea who
 * subscribes, or whether anyone does.
 *
 * <p>{@code now} is a parameter rather than read from the system clock so
 * the time-window rules below are unit-testable at exact boundaries.
 */
@Component
public class InterviewEventDetector {

    /** FR-99: "an interview is due within 24 hours." */
    static final Duration UPCOMING_WINDOW = Duration.ofHours(24);

    /**
     * How long after an interview's scheduled end a debrief is considered
     * missing: long enough that nobody is nagged while still walking out of
     * the room, short enough that the details are still fresh (US-78).
     */
    static final Duration DEBRIEF_GRACE = Duration.ofHours(2);

    /**
     * Interviews older than this are not nudged about: after two weeks the
     * "while the details are still fresh" rationale no longer holds, and
     * nudging about a months-old interview (e.g. freshly back-filled
     * history) would be noise, not help.
     */
    static final Duration DEBRIEF_LOOKBACK = Duration.ofDays(14);

    /** Assumed interview length when {@code duration_minutes} was never recorded. */
    static final Duration DEFAULT_INTERVIEW_LENGTH = Duration.ofHours(1);

    private final InterviewRepository interviewRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationEventPublisher eventPublisher;

    public InterviewEventDetector(
            InterviewRepository interviewRepository,
            InterviewFeedbackRepository feedbackRepository,
            ApplicationRepository applicationRepository,
            CompanyRepository companyRepository,
            ApplicationEventPublisher eventPublisher) {
        this.interviewRepository = interviewRepository;
        this.feedbackRepository = feedbackRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.eventPublisher = eventPublisher;
    }

    public record DetectionSummary(int upcomingInterviews, int missingDebriefs) {
    }

    @Transactional(readOnly = true)
    public DetectionSummary detect(UUID userId, Instant now) {
        List<Interview> scheduled = interviewRepository.findAllByUserId(userId).stream()
                .filter(interview -> interview.getScheduledAt() != null)
                .toList();
        if (scheduled.isEmpty()) {
            return new DetectionSummary(0, 0);
        }

        Map<UUID, Application> applicationsById = applicationRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(Application::getId, Function.identity()));
        Map<UUID, Company> companiesById = companyRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(Company::getId, Function.identity()));
        Map<UUID, InterviewFeedback> feedbackByInterview = feedbackRepository
                .findAllByUserIdAndInterviewIdIn(userId, scheduled.stream().map(Interview::getId).toList())
                .stream()
                .collect(Collectors.toMap(InterviewFeedback::getInterviewId, Function.identity()));

        int upcoming = 0;
        int missing = 0;
        for (Interview interview : scheduled) {
            Application application = applicationsById.get(interview.getApplicationId());
            if (application == null) {
                continue;
            }
            Company company = companiesById.get(application.getCompanyId());
            String companyName = company == null ? "Unknown company" : company.getName();

            if (isUpcoming(interview, application, now)) {
                eventPublisher.publishEvent(new UpcomingInterviewEvent(
                        userId, interview.getId(), application.getId(), companyName, application.getJobTitle(),
                        interview.getRoundNumber(), interview.getRoundType(), interview.getScheduledAt()));
                upcoming++;
            }

            InterviewFeedback feedback = feedbackByInterview.get(interview.getId());
            if (isDebriefMissing(interview, feedback, now)) {
                eventPublisher.publishEvent(new MissingDebriefEvent(
                        userId, interview.getId(), application.getId(), companyName, application.getJobTitle(),
                        interview.getRoundNumber(), interview.getRoundType(), interview.getScheduledAt(),
                        feedback != null));
                missing++;
            }
        }
        return new DetectionSummary(upcoming, missing);
    }

    /**
     * Due within the next 24 hours, still awaiting an outcome, and attached
     * to an application that is still live -- an interview on a withdrawn
     * or rejected application is almost certainly cancelled, and reminding
     * about it would be wrong, not merely noisy.
     */
    static boolean isUpcoming(Interview interview, Application application, Instant now) {
        Instant scheduledAt = interview.getScheduledAt();
        return scheduledAt.isAfter(now)
                && !scheduledAt.isAfter(now.plus(UPCOMING_WINDOW))
                && interview.getOutcome() == Interview.Outcome.pending
                && !application.getStatus().isTerminal();
    }

    /**
     * The interview ended at least {@link #DEBRIEF_GRACE} ago, it is no
     * older than {@link #DEBRIEF_LOOKBACK}, and no debrief has been
     * <em>submitted</em> for it (a saved draft still counts as missing --
     * US-30's draft/submit split means a draft is, by definition, a debrief
     * the user has not finished yet).
     */
    static boolean isDebriefMissing(Interview interview, InterviewFeedback feedback, Instant now) {
        Duration length = interview.getDurationMinutes() == null
                ? DEFAULT_INTERVIEW_LENGTH
                : Duration.ofMinutes(interview.getDurationMinutes());
        Instant endedAt = interview.getScheduledAt().plus(length);
        boolean pastGrace = !endedAt.plus(DEBRIEF_GRACE).isAfter(now);
        boolean recentEnough = !interview.getScheduledAt().isBefore(now.minus(DEBRIEF_LOOKBACK));
        boolean submitted = feedback != null && !feedback.isDraft();
        return pastGrace && recentEnough && !submitted;
    }
}
