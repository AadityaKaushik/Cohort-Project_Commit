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

@Component
public class NotificationDetectionJob {
    private static final Logger log = LoggerFactory.getLogger(NotificationDetectionJob.class);

    private final UserRepository userRepository;
    private final StalledApplicationDetector stalledApplicationDetector;
    private final InterviewEventDetector interviewEventDetector;
    private final ColdContactDetector coldContactDetector;

    public NotificationDetectionJob(
            UserRepository userRepository,
            StalledApplicationDetector stalledApplicationDetector,
            InterviewEventDetector interviewEventDetector,
            ColdContactDetector coldContactDetector) {
        this.userRepository = userRepository;
        this.stalledApplicationDetector = stalledApplicationDetector;
        this.interviewEventDetector = interviewEventDetector;
        this.coldContactDetector = coldContactDetector;
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

    public RunSummary runDetection(Instant now) {
        List<User> users = userRepository.findAllByIsActiveTrue();
        int stalled = 0;
        int upcoming = 0;
        int missing = 0;
        int cold = 0;
        int failed = 0;
        for (User user : users) {
            try {
                UserSummary result = runFor(user.getId(), now);
                stalled += result.stalledApplications();
                upcoming += result.upcomingInterviews();
                missing += result.missingDebriefs();
                cold += result.coldContacts();
            } catch (RuntimeException ex) {
                failed++;
                log.warn("Notification detection failed for user {}: {}", user.getId(), ex.getMessage());
            }
        }
        return new RunSummary(users.size(), stalled, upcoming, missing, cold, failed);
    }

    public UserSummary runFor(UUID userId, Instant now) {
        int stalled = stalledApplicationDetector.detect(userId);
        InterviewEventDetector.DetectionSummary interviews = interviewEventDetector.detect(userId, now);
        int cold = coldContactDetector.detect(userId, now);
        return new UserSummary(stalled, interviews.upcomingInterviews(), interviews.missingDebriefs(), cold);
    }

    public record UserSummary(int stalledApplications, int upcomingInterviews, int missingDebriefs, int coldContacts) {
    }

    public record RunSummary(int usersScanned, int stalledApplications, int upcomingInterviews, int missingDebriefs, int coldContacts, int failedUsers) {
    }
}
