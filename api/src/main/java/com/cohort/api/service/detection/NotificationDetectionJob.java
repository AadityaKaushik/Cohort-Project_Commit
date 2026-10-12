package com.cohort.api.service.detection;

import com.cohort.api.entity.User;
import com.cohort.api.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * US-76..78 / §4.3 step 1: the scheduled trigger that makes detection run
 * in normal use. Every cycle, each active user's data goes through both
 * detectors, which publish whatever events apply; what happens to those
 * events is not this job's concern (Observer). Re-running is always safe:
 * each event carries an occurrence key, and the in-app channel stores at
 * most one notification per key, so the same stall or the same interview
 * reminder is never notified twice however often this runs.
 *
 * <p>One user's failure is logged and skipped, never allowed to abort the
 * rest of the batch -- the same "best effort background sync" stance as
 * {@code FlFeatureExportJob}.
 */
@Component
public class NotificationDetectionJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationDetectionJob.class);

    private final UserRepository userRepository;
    private final StalledApplicationDetector stalledApplicationDetector;
    private final InterviewEventDetector interviewEventDetector;

    public NotificationDetectionJob(
            UserRepository userRepository,
            StalledApplicationDetector stalledApplicationDetector,
            InterviewEventDetector interviewEventDetector) {
        this.userRepository = userRepository;
        this.stalledApplicationDetector = stalledApplicationDetector;
        this.interviewEventDetector = interviewEventDetector;
    }

    @Scheduled(
            initialDelayString = "${app.notifications.detection.initial-delay:PT1M}",
            fixedDelayString = "${app.notifications.detection.interval:PT15M}")
    public void detectOnSchedule() {
        RunSummary summary = runDetection(Instant.now());
        if (summary.failedUsers() > 0) {
            log.warn("Notification detection finished with {} failed user(s): {}", summary.failedUsers(), summary);
        } else {
            log.debug("Notification detection finished: {}", summary);
        }
    }

    /** The actual work, callable directly (tests, or a future admin trigger) without waiting for the schedule. */
    public RunSummary runDetection(Instant now) {
        List<User> users = userRepository.findAllByIsActiveTrue();
        int stalled = 0;
        int upcoming = 0;
        int missing = 0;
        int failed = 0;
        for (User user : users) {
            try {
                UserSummary result = runFor(user.getId(), now);
                stalled += result.stalledApplications();
                upcoming += result.upcomingInterviews();
                missing += result.missingDebriefs();
            } catch (RuntimeException ex) {
                failed++;
                log.warn("Notification detection failed for user {}: {}", user.getId(), ex.getMessage());
            }
        }
        return new RunSummary(users.size(), stalled, upcoming, missing, failed);
    }

    public UserSummary runFor(UUID userId, Instant now) {
        int stalled = stalledApplicationDetector.detect(userId);
        InterviewEventDetector.DetectionSummary interviews = interviewEventDetector.detect(userId, now);
        return new UserSummary(stalled, interviews.upcomingInterviews(), interviews.missingDebriefs());
    }

    public record UserSummary(int stalledApplications, int upcomingInterviews, int missingDebriefs) {
    }

    public record RunSummary(int usersScanned, int stalledApplications, int upcomingInterviews, int missingDebriefs, int failedUsers) {
    }
}
