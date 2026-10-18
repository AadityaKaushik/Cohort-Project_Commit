package com.cohort.api.service.fl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cohort.api.entity.User;
import com.cohort.api.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlFeatureExportJobTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private FlFeatureExtractionService extractionService;
    @Mock
    private FlExportClient exportClient;

    private FlFeatureExportJob job;

    @Test
    void exportsOnlyUsersReturnedByFindAllByFlOptInTrue() {
        job = new FlFeatureExportJob(userRepository, extractionService, exportClient);
        User optedIn = user();
        when(userRepository.findAllByFlOptInTrue()).thenReturn(List.of(optedIn));
        when(extractionService.extract(org.mockito.ArgumentMatchers.eq(optedIn.getId()), any())).thenReturn(zeroVector());

        FlFeatureExportJob.ExportSummary summary = job.runExport();

        verify(exportClient, times(1)).export(org.mockito.ArgumentMatchers.eq(optedIn.getId()), any(), any());
        assertThat(summary.eligible()).isEqualTo(1);
        assertThat(summary.succeeded()).isEqualTo(1);
        assertThat(summary.failed()).isEqualTo(0);
    }

    @Test
    void sendsTheUsersLabelledRoundsAlongWithTheCurrentVector() {
        job = new FlFeatureExportJob(userRepository, extractionService, exportClient);
        User optedIn = user();
        List<FlTrainingExample> rated = List.of(new FlTrainingExample(zeroVector(), true), new FlTrainingExample(zeroVector(), false));
        when(userRepository.findAllByFlOptInTrue()).thenReturn(List.of(optedIn));
        when(extractionService.extract(org.mockito.ArgumentMatchers.eq(optedIn.getId()), any())).thenReturn(zeroVector());
        when(extractionService.trainingExamples(optedIn.getId())).thenReturn(rated);

        job.runExport();

        verify(exportClient).export(optedIn.getId(), zeroVector(), rated);
    }

    @Test
    void anOptedOutUserIsNeverPassedToExtractionOrExport() {
        job = new FlFeatureExportJob(userRepository, extractionService, exportClient);
        // Opted-out users are never returned by the repository.
        when(userRepository.findAllByFlOptInTrue()).thenReturn(List.of());

        FlFeatureExportJob.ExportSummary summary = job.runExport();

        verify(extractionService, never()).extract(any(), any());
        verify(exportClient, never()).export(any(), any(), any());
        assertThat(summary.eligible()).isEqualTo(0);
    }

    @Test
    void oneUsersExportFailureDoesNotStopTheRestOfTheBatch() {
        job = new FlFeatureExportJob(userRepository, extractionService, exportClient);
        User failing = user();
        User succeeding = user();
        when(userRepository.findAllByFlOptInTrue()).thenReturn(List.of(failing, succeeding));
        when(extractionService.extract(any(), any())).thenReturn(zeroVector());
        doThrow(new RuntimeException("fl-service unreachable")).when(exportClient).export(org.mockito.ArgumentMatchers.eq(failing.getId()), any(), any());

        FlFeatureExportJob.ExportSummary summary = job.runExport();

        verify(exportClient).export(org.mockito.ArgumentMatchers.eq(succeeding.getId()), any(), any());
        assertThat(summary.eligible()).isEqualTo(2);
        assertThat(summary.succeeded()).isEqualTo(1);
        assertThat(summary.failed()).isEqualTo(1);
    }

    private User user() {
        User user = new User("user" + UUID.randomUUID() + "@example.com", "hash", "Test User");
        setId(user, UUID.randomUUID());
        return user;
    }

    // The id is normally set by Hibernate on persist, so set it by reflection.
    private void setId(User user, UUID id) {
        try {
            java.lang.reflect.Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private FlFeatureVector zeroVector() {
        return new FlFeatureVector(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
