package com.cohort.api.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.Notification;
import com.cohort.api.repository.UserRepository;
import com.cohort.api.service.application.StallDetectionService;
import com.cohort.api.service.application.StallDetectionService.StallEvaluation;
import com.cohort.api.service.application.StallDetectionService.StalledApplication;
import com.cohort.api.service.detection.StalledApplicationDetector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class NotificationStrategySwapTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID applicationId = UUID.randomUUID();

    @Test
    void swappingTheChannelReroutesDeliveryWithoutTouchingTheDetector() {
        RecordingNotificationChannel inAppLike = new RecordingNotificationChannel("in_app");
        RecordingNotificationChannel emailLike = new RecordingNotificationChannel("email");

        runDetectionWith(List.of(inAppLike, emailLike), List.of("in_app"));
        runDetectionWith(List.of(inAppLike, emailLike), List.of("email"));

        assertThat(inAppLike.delivered).hasSize(1);
        assertThat(emailLike.delivered).hasSize(1);
        assertThat(emailLike.delivered.get(0)).isEqualTo(inAppLike.delivered.get(0));
        NotificationMessage message = emailLike.delivered.get(0);
        assertThat(message.userId()).isEqualTo(userId);
        assertThat(message.type()).isEqualTo(Notification.Type.stalled_application);
        assertThat(message.relatedEntityId()).isEqualTo(applicationId);
    }

    @Test
    void enablingTwoStrategiesDeliversTheSameMessageThroughBoth() {
        RecordingNotificationChannel inAppLike = new RecordingNotificationChannel("in_app");
        RecordingNotificationChannel pushLike = new RecordingNotificationChannel("push");

        runDetectionWith(List.of(inAppLike, pushLike), List.of("in_app", "push"));

        assertThat(inAppLike.delivered).hasSize(1);
        assertThat(pushLike.delivered).containsExactlyElementsOf(inAppLike.delivered);
    }

    @Test
    void aFailingChannelNeitherBlocksTheOthersNorReachesTheDetector() {
        NotificationChannel broken = new NotificationChannel() {
            @Override
            public String id() {
                return "broken";
            }

            @Override
            public boolean deliver(NotificationMessage message) {
                throw new IllegalStateException("SMTP relay down");
            }
        };
        RecordingNotificationChannel inAppLike = new RecordingNotificationChannel("in_app");

        int published = runDetectionWith(List.of(broken, inAppLike), List.of("broken", "in_app"));

        assertThat(published).isEqualTo(1);
        assertThat(inAppLike.delivered).hasSize(1);
    }

    @Test
    void theContextDependsOnlyOnTheStrategyInterfaceNeverOnAConcreteChannel() {
        for (Class<?> type : List.of(NotificationService.class, NotificationListener.class, StalledApplicationDetector.class)) {
            for (Field field : type.getDeclaredFields()) {
                assertThat(NotificationChannel.class.isAssignableFrom(field.getType()) && !field.getType().isInterface())
                        .as("%s.%s is a concrete channel", type.getSimpleName(), field.getName())
                        .isFalse();
            }
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                assertThat(Arrays.asList(constructor.getParameterTypes())).doesNotContain(InAppNotificationChannel.class);
            }
        }
    }

    @Test
    void anUnknownConfiguredChannelFailsFastAtStartup() {
        assertThatThrownBy(() -> new NotificationService(List.of(new RecordingNotificationChannel("in_app")), List.of("sms")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sms");
    }

    // Fresh Spring context per channel configuration.
    private int runDetectionWith(List<NotificationChannel> channels, List<String> enabled) {
        StallDetectionService stallDetectionService = mock(StallDetectionService.class);
        when(stallDetectionService.detectStalled(userId)).thenReturn(List.of(new StalledApplication(
                applicationId, "Acme", "Backend Engineer", ApplicationStatus.APPLIED,
                new StallEvaluation(true, 21, 14, StallDetectionService.BASIS_SYSTEM_DEFAULT, 0),
                Instant.parse("2026-09-01T00:00:00Z"))));
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(StallDetectionService.class, () -> stallDetectionService);
            context.registerBean(UserRepository.class, () -> userRepository);
            context.registerBean(NotificationService.class, () -> new NotificationService(channels, enabled));
            context.registerBean(NotificationListener.class);
            context.registerBean(StalledApplicationDetector.class);
            context.refresh();

            return context.getBean(StalledApplicationDetector.class).detect(userId);
        }
    }
}
