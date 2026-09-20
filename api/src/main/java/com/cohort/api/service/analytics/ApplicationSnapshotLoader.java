package com.cohort.api.service.analytics;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatusHistory;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.ApplicationStatusHistoryRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Loads every one of a user's applications plus each one's full status
 * history, scoped end to end by {@code userId} (FR-94/95), as the shared
 * {@link ApplicationSnapshot} shape every {@code AnalyticsMetric} in this
 * package -- and, since P36, {@code StallDetectionService} -- computes
 * from. Pulled out of {@code AnalyticsService} once a second consumer
 * needed the exact same loading logic, rather than each service querying
 * the repositories itself.
 */
@Component
public class ApplicationSnapshotLoader {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;

    public ApplicationSnapshotLoader(ApplicationRepository applicationRepository, ApplicationStatusHistoryRepository historyRepository) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
    }

    public List<ApplicationSnapshot> load(UUID userId) {
        List<Application> applications = applicationRepository.findAllByUserId(userId);
        List<UUID> applicationIds = applications.stream().map(Application::getId).toList();

        Map<UUID, List<ApplicationStatusHistory>> historyByApplication = applicationIds.isEmpty()
                ? Map.of()
                : historyRepository.findByApplicationIdIn(applicationIds).stream()
                        .collect(Collectors.groupingBy(ApplicationStatusHistory::getApplicationId));

        return applications.stream()
                .map(application -> new ApplicationSnapshot(
                        application.getId(),
                        application.getStatus(),
                        application.getCurrentStage(),
                        application.getStageEnteredAt(),
                        historyByApplication.getOrDefault(application.getId(), List.of())))
                .toList();
    }
}
