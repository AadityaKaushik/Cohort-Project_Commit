package com.cohort.api.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.InterviewRoundType;
import com.cohort.api.entity.Notification;
import com.cohort.api.entity.User;
import com.cohort.api.event.MissingDebriefEvent;
import com.cohort.api.event.StalledApplicationEvent;
import com.cohort.api.event.UpcomingInterviewEvent;
import com.cohort.api.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The copy each event turns into, and that every message stays scoped to the event's own user and target (FR-100). */
class NotificationListenerTest {

    private final UUID userId = UUID.randomUUID();
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RecordingNotificationChannel channel = new RecordingNotificationChannel("in_app");
    private final NotificationListener listener =
            new NotificationListener(new NotificationService(List.of(channel), List.of("in_app")), userRepository);

    @Test
    void aStalledApplicationPointsAtTheApplicationAndExplainsTheThreshold() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        UUID applicationId = UUID.randomUUID();
        StalledApplicationEvent event = new StalledApplicationEvent(
                userId, applicationId, "Acme", "Backend Engineer", ApplicationStatus.APPLIED,
                Instant.parse("2026-09-01T00:00:00Z"), 20.7, 14.0);

        listener.onStalledApplication(event);

        NotificationMessage message = channel.delivered.get(0);
        assertThat(message.userId()).isEqualTo(userId);
        assertThat(message.type()).isEqualTo(Notification.Type.stalled_application);
        assertThat(message.title()).isEqualTo("Your Acme application has stalled");
        assertThat(message.body()).isEqualTo("\"Backend Engineer\" has been at the Applied stage for 20 days, past the "
                + "14-day point where it counts as stalled. Consider following up, or update its status if anything has changed.");
        assertThat(message.relatedEntityType()).isEqualTo(Notification.RelatedEntityType.application);
        assertThat(message.relatedEntityId()).isEqualTo(applicationId);
        assertThat(message.dedupeKey()).isEqualTo(event.occurrenceKey());
    }

    @Test
    void anUpcomingInterviewIsShownInTheUsersOwnTimezone() {
        User user = new User("tz@example.com", "hash", "Tz");
        user.setTimezone("Asia/Kolkata");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        UUID interviewId = UUID.randomUUID();

        listener.onUpcomingInterview(new UpcomingInterviewEvent(
                userId, interviewId, UUID.randomUUID(), "Globex", "SRE", (short) 2, InterviewRoundType.SYSTEM_DESIGN,
                Instant.parse("2026-09-26T08:30:00Z")));

        NotificationMessage message = channel.delivered.get(0);
        assertThat(message.title()).isEqualTo("Interview coming up with Globex");
        assertThat(message.body()).startsWith("Round 2 (System design) for \"SRE\" is scheduled for Sat 26 Sep, 14:00 (Asia/Kolkata).");
        assertThat(message.relatedEntityType()).isEqualTo(Notification.RelatedEntityType.interview);
        assertThat(message.relatedEntityId()).isEqualTo(interviewId);
    }

    @Test
    void timesFallBackToLabelledUtcWhenTheProfileHasNoValidTimezone() {
        User user = new User("tz2@example.com", "hash", "Tz");
        user.setTimezone("Not/A_Zone");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        listener.onUpcomingInterview(new UpcomingInterviewEvent(
                userId, UUID.randomUUID(), UUID.randomUUID(), "Globex", "SRE", (short) 1, InterviewRoundType.HR,
                Instant.parse("2026-09-26T08:30:00Z")));

        assertThat(channel.delivered.get(0).body()).contains("Sat 26 Sep, 08:30 (UTC)");
    }

    @Test
    void aMissingDebriefIsWordedDifferentlyWhenADraftAlreadyExists() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Instant when = Instant.parse("2026-09-23T10:00:00Z");

        listener.onMissingDebrief(new MissingDebriefEvent(userId, first, UUID.randomUUID(), "Initech", "QA", (short) 1,
                InterviewRoundType.TECHNICAL, when, false));
        listener.onMissingDebrief(new MissingDebriefEvent(userId, second, UUID.randomUUID(), "Initech", "QA", (short) 2,
                InterviewRoundType.BEHAVIOURAL, when, true));

        assertThat(channel.delivered).extracting(NotificationMessage::title)
                .containsExactly("Debrief your Initech interview", "Finish your Initech debrief");
        assertThat(channel.delivered.get(0).body())
                .isEqualTo("Round 1 (Technical) for \"QA\" was on Wed 23 Sep. Log how it went while the details are still fresh.");
        assertThat(channel.delivered.get(1).body()).startsWith("You started a debrief for Round 2 (Behavioural) on Wed 23 Sep");
        assertThat(channel.delivered).extracting(NotificationMessage::relatedEntityId).containsExactly(first, second);
    }
}
