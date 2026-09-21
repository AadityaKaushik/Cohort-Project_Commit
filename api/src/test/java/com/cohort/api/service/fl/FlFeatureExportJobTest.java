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

/**
 * US-57/US-58/US-64: the export job's whole privacy contract is "only
 * opted-in users are ever exported, and an opt-out takes effect on the
 * next run" — enforced here by never even asking {@link FlFeatureExtractionService}/
 * {@link FlExportClient} about a user {@link UserRepository#findAllByFlOptInTrue()}
 * didn't return, not by filtering after the fact.
 */
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

        verify(exportClient, times(1)).export(org.mockito.ArgumentMatchers.eq(optedIn.getId()), any());
        assertThat(summary.eligible()).isEqualTo(1);
        assertThat(summary.succeeded()).isEqualTo(1);
        assertThat(summary.failed()).isEqualTo(0);
    }

    @Test
    void anOptedOutUserIsNeverPassedToExtractionOrExport() {
        job = new FlFeatureExportJob(userRepository, extractionService, exportClient);
        // The repository call itself is the opt-in gate -- an opted-out user
        // is simply never in this list, so the job never even sees their id.
        when(userRepository.findAllByFlOptInTrue()).thenReturn(List.of());

        FlFeatureExportJob.ExportSummary summary = job.runExport();

        verify(extractionService, never()).extract(any(), any());
        verify(exportClient, never()).export(any(), any());
        assertThat(summary.eligible()).isEqualTo(0);
    }

    @Test
    void oneUsersExportFailureDoesNotStopTheRestOfTheBatch() {
        job = new FlFeatureExportJob(userRepository, extractionService, exportClient);
        User failing = user();
        User succeeding = user();
        when(userRepository.findAllByFlOptInTrue()).thenReturn(List.of(failing, succeeding));
        when(extractionService.extract(any(), any())).thenReturn(zeroVector());
        doThrow(new RuntimeException("fl-service unreachable")).when(exportClient).export(org.mockito.ArgumentMatchers.eq(failing.getId()), any());

        FlFeatureExportJob.ExportSummary summary = job.runExport();

        verify(exportClient).export(org.mockito.ArgumentMatchers.eq(succeeding.getId()), any());
        assertThat(summary.eligible()).isEqualTo(2);
        assertThat(summary.succeeded()).isEqualTo(1);
        assertThat(summary.failed()).isEqualTo(1);
    }

    private User user() {
        User user = new User("user" + UUID.randomUUID() + "@example.com", "hash", "Test User");
        setId(user, UUID.randomUUID());
        return user;
    }

    /** id is @UuidGenerator-managed (only set by Hibernate on persist); this user is never persisted in this unit test, so it's set via reflection instead. */
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
