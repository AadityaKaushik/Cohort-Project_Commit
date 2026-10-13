package com.cohort.api.mapper;

import com.cohort.api.dto.ApplicationDetailResponse;
import com.cohort.api.dto.ApplicationInterviewSummary;
import com.cohort.api.dto.ApplicationStatusHistoryResponse;
import com.cohort.api.dto.ApplicationSummary;
import com.cohort.api.dto.CompanySummary;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationStatusHistory;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.Interview;
import com.cohort.api.service.application.state.ApplicationStatusStateRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * DTO + Mapper pattern (ARCHITECTURE_REVISION_JAVA.md §4.1): hand-written
 * rather than MapStruct, matching the style already established by
 * UserProfileResponse.from() et al. in this codebase. JPA entities never
 * cross the controller boundary — everything a controller returns comes
 * through here.
 */
@Component
public class ApplicationMapper {

    private final ApplicationStatusStateRegistry stateRegistry;

    public ApplicationMapper(ApplicationStatusStateRegistry stateRegistry) {
        this.stateRegistry = stateRegistry;
    }

    public ApplicationSummary toSummary(Application application, Company company) {
        return new ApplicationSummary(
                application.getId(),
                CompanySummary.from(company),
                application.getJobTitle(),
                application.getStatus(),
                application.getCurrentStage(),
                daysInStage(application),
                application.getDateApplied(),
                application.getPriority(),
                application.getArchivedAt() != null,
                sorted(stateRegistry.stateFor(application.getStatus()).allowedTransitions()),
                sorted(stateRegistry.stateFor(application.getStatus()).correctionTransitions()));
    }

    public ApplicationDetailResponse toDetail(
            Application application,
            Company company,
            List<ApplicationStatusHistory> history,
            List<Interview> interviews) {
        List<ApplicationStatusHistoryResponse> historyResponse =
                history.stream().map(ApplicationStatusHistoryResponse::from).toList();
        List<ApplicationInterviewSummary> interviewSummaries =
                interviews.stream().map(ApplicationInterviewSummary::from).toList();

        return new ApplicationDetailResponse(
                application.getId(),
                CompanySummary.from(company),
                application.getResumeId(),
                application.getJobTitle(),
                application.getJobDescription(),
                application.getJobUrl(),
                application.getLocation(),
                application.getWorkMode(),
                application.getJobType(),
                application.getSalaryMin(),
                application.getSalaryMax(),
                application.getSalaryCurrency(),
                application.getDateApplied(),
                application.getStatus(),
                application.getCurrentStage(),
                daysInStage(application),
                application.getPriority(),
                application.getSource(),
                application.isReferral(),
                application.getDeadline(),
                application.getRecruiterName(),
                application.getRecruiterEmail(),
                application.getNotes(),
                application.getCoverLetter(),
                application.getArchivedAt(),
                application.getStageEnteredAt(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                historyResponse,
                interviewSummaries,
                sorted(stateRegistry.stateFor(application.getStatus()).allowedTransitions()),
                sorted(stateRegistry.stateFor(application.getStatus()).correctionTransitions()));
    }

    private static List<ApplicationStatus> sorted(java.util.Set<ApplicationStatus> statuses) {
        return statuses.stream().sorted().toList();
    }

    private long daysInStage(Application application) {
        return Duration.between(application.getStageEnteredAt(), Instant.now()).toDays();
    }
}
