package com.cohort.api.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cohort.api.entity.Notification;
import com.cohort.api.repository.NotificationRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InAppNotificationChannelTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final InAppNotificationChannel channel = new InAppNotificationChannel(repository);

    @Test
    void persistsTheMessageScopedToItsUserWithTheInAppChannelTag() {
        UUID userId = UUID.randomUUID();
        UUID interviewId = UUID.randomUUID();
        when(repository.insertIfAbsent(any(), anyString(), anyString(), anyString(), any(), any(), anyString(), any())).thenReturn(1);

        boolean delivered = channel.deliver(new NotificationMessage(
                userId, Notification.Type.upcoming_interview, "Title", "Body",
                Notification.RelatedEntityType.interview, interviewId, "upcoming_interview:x"));

        assertThat(delivered).isTrue();
        verify(repository).insertIfAbsent(userId, "upcoming_interview", "Title", "Body", "interview",
                interviewId.toString(), "in_app", "upcoming_interview:x");
    }

    @Test
    void reportsNothingNewWhenTheOccurrenceWasAlreadyStored() {
        when(repository.insertIfAbsent(any(), anyString(), anyString(), anyString(), any(), any(), anyString(), any())).thenReturn(0);

        boolean delivered = channel.deliver(new NotificationMessage(
                UUID.randomUUID(), Notification.Type.missing_debrief, "T", "B", null, null, "k"));

        assertThat(delivered).isFalse();
        verify(repository).insertIfAbsent(any(), eq("missing_debrief"), eq("T"), eq("B"), isNull(), isNull(), eq("in_app"), eq("k"));
    }

    @Test
    void identifiesItselfAsInApp() {
        assertThat(channel.id()).isEqualTo("in_app");
    }
}
