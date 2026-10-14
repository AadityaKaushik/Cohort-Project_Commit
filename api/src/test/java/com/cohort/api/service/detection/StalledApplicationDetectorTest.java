package com.cohort.api.service.detection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.event.StalledApplicationEvent;
import com.cohort.api.service.application.StallDetectionService;
import com.cohort.api.service.application.StallDetectionService.StallEvaluation;
import com.cohort.api.service.application.StallDetectionService.StalledApplication;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StalledApplicationDetectorTest {
    private final StallDetectionService stallDetectionService = mock(StallDetectionService.class);
    private final RecordingEventPublisher publisher = new RecordingEventPublisher();
    private final StalledApplicationDetector detector = new StalledApplicationDetector(stallDetectionService, publisher);

    @Test
    void publishesOneEventPerStalledApplicationCarryingTheStallFacts() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        Instant enteredAt = Instant.parse("2026-09-01T10:00:00Z");
        StallEvaluation evaluation = new StallEvaluation(true, 20.5, 14.0, StallDetectionService.BASIS_SYSTEM_DEFAULT, 0);
        when(stallDetectionService.detectStalled(userId)).thenReturn(List.of(
                new StalledApplication(applicationId, "Acme", "Backend Engineer", ApplicationStatus.APPLIED, evaluation, enteredAt)));

        int published = detector.detect(userId);

        assertThat(published).isEqualTo(1);
        assertThat(publisher.published).containsExactly(new StalledApplicationEvent(
                userId, applicationId, "Acme", "Backend Engineer", ApplicationStatus.APPLIED, enteredAt, 20.5, 14.0));
    }

    @Test
    void publishesNothingWhenNoApplicationIsStalled() {
        UUID userId = UUID.randomUUID();
        when(stallDetectionService.detectStalled(userId)).thenReturn(List.of());

        assertThat(detector.detect(userId)).isZero();
        assertThat(publisher.published).isEmpty();
    }

    @Test
    void theSameStallAlwaysYieldsTheSameOccurrenceKeyButANewStageStayDoesNot() {
        UUID applicationId = UUID.randomUUID();
        Instant firstStay = Instant.parse("2026-09-01T10:00:00Z");
        StalledApplicationEvent first = new StalledApplicationEvent(
                UUID.randomUUID(), applicationId, "Acme", "SWE", ApplicationStatus.APPLIED, firstStay, 20, 14);
        StalledApplicationEvent sameStallSeenLater = new StalledApplicationEvent(
                first.userId(), applicationId, "Acme", "SWE", ApplicationStatus.APPLIED, firstStay, 27, 14);
        StalledApplicationEvent laterStage = new StalledApplicationEvent(
                first.userId(), applicationId, "Acme", "SWE", ApplicationStatus.OA, firstStay.plusSeconds(86_400 * 30), 12, 10);

        assertThat(sameStallSeenLater.occurrenceKey()).isEqualTo(first.occurrenceKey());
        assertThat(laterStage.occurrenceKey()).isNotEqualTo(first.occurrenceKey());
    }

    @Test
    void theDetectorDependsOnNoSubscriberOnlyOnTheGenericPublisher() {
        DetectorIndependenceAssertions.assertKnowsNoSubscriber(StalledApplicationDetector.class);
    }
}
