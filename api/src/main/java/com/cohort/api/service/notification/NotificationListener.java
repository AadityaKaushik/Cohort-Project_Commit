package com.cohort.api.service.notification;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.InterviewRoundType;
import com.cohort.api.entity.Notification;
import com.cohort.api.event.MissingDebriefEvent;
import com.cohort.api.event.StalledApplicationEvent;
import com.cohort.api.event.UpcomingInterviewEvent;
import com.cohort.api.repository.UserRepository;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer pattern, subscriber side (ARCHITECTURE_REVISION_JAVA.md §4.1/§4.3,
 * FR-100): subscribes to the three P59 domain events, turns each into
 * user-facing copy, and hands the resulting {@link NotificationMessage} to
 * {@link NotificationService}, which picks the delivery strategy. The
 * detectors that publish these events never learn this class exists; it is
 * discovered purely through Spring's {@code @EventListener} registration.
 *
 * <p>Times are rendered in the user's own profile timezone when one is set
 * (US-05), falling back to UTC -- labelled as such so it's never ambiguous.
 */
@Component
public class NotificationListener {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationListener(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @EventListener
    public void onStalledApplication(StalledApplicationEvent event) {
        long days = (long) Math.floor(event.daysInStage());
        long threshold = Math.round(event.thresholdDays());
        String title = "Your " + event.companyName() + " application has stalled";
        String body = "\"" + event.jobTitle() + "\" has been at the " + stageLabel(event.stage()) + " stage for "
                + days + " days, past the " + threshold + "-day point where it counts as stalled. "
                + "Consider following up, or update its status if anything has changed.";
        notificationService.dispatch(new NotificationMessage(
                event.userId(), Notification.Type.stalled_application, title, body,
                Notification.RelatedEntityType.application, event.applicationId(), event.occurrenceKey()));
    }

    @EventListener
    public void onUpcomingInterview(UpcomingInterviewEvent event) {
        String title = "Interview coming up with " + event.companyName();
        String body = roundLabel(event.roundNumber(), event.roundType()) + " for \"" + event.jobTitle()
                + "\" is scheduled for " + formatDateTime(event.userId(), event.scheduledAt())
                + ". A quick review of your prep notes and the job description will help.";
        notificationService.dispatch(new NotificationMessage(
                event.userId(), Notification.Type.upcoming_interview, title, body,
                Notification.RelatedEntityType.interview, event.interviewId(), event.occurrenceKey()));
    }

    @EventListener
    public void onMissingDebrief(MissingDebriefEvent event) {
        String when = DATE.format(event.scheduledAt().atZone(zoneFor(event.userId())));
        String title = event.draftStarted()
                ? "Finish your " + event.companyName() + " debrief"
                : "Debrief your " + event.companyName() + " interview";
        String body = event.draftStarted()
                ? "You started a debrief for " + roundLabel(event.roundNumber(), event.roundType()) + " on " + when
                        + " but haven't submitted it yet. Submitting it keeps your prep insights accurate."
                : roundLabel(event.roundNumber(), event.roundType()) + " for \"" + event.jobTitle() + "\" was on " + when
                        + ". Log how it went while the details are still fresh.";
        notificationService.dispatch(new NotificationMessage(
                event.userId(), Notification.Type.missing_debrief, title, body,
                Notification.RelatedEntityType.interview, event.interviewId(), event.occurrenceKey()));
    }

    private String formatDateTime(UUID userId, Instant instant) {
        ZoneId zone = zoneFor(userId);
        String zoneLabel = zone.equals(ZoneOffset.UTC) ? "UTC" : zone.getId();
        return DATE_TIME.format(instant.atZone(zone)) + " (" + zoneLabel + ")";
    }

    private ZoneId zoneFor(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getTimezone())
                .filter(timezone -> timezone != null && !timezone.isBlank())
                .map(timezone -> {
                    try {
                        return ZoneId.of(timezone);
                    } catch (DateTimeException invalid) {
                        return null;
                    }
                })
                .orElse(ZoneOffset.UTC);
    }

    static String stageLabel(ApplicationStatus stage) {
        return switch (stage) {
            case SAVED -> "Saved";
            case APPLIED -> "Applied";
            case OA -> "Online assessment";
            case INTERVIEW -> "Interview";
            case FINAL_ROUND -> "Final round";
            case OFFER -> "Offer";
            case REJECTED -> "Rejected";
            case WITHDRAWN -> "Withdrawn";
            case ACCEPTED -> "Accepted";
            case DECLINED -> "Declined";
        };
    }

    static String roundLabel(short roundNumber, InterviewRoundType roundType) {
        String type = switch (roundType) {
            case OA -> "Online assessment";
            case PHONE_SCREEN -> "Phone screen";
            case TECHNICAL -> "Technical";
            case SYSTEM_DESIGN -> "System design";
            case BEHAVIOURAL -> "Behavioural";
            case HR -> "HR";
            case FINAL -> "Final";
        };
        return "Round " + roundNumber + " (" + type + ")";
    }
}
