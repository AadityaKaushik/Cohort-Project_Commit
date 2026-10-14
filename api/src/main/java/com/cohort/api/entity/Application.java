package com.cohort.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "applications")
public class Application {
    public enum WorkMode { onsite, hybrid, remote }

    public enum JobType { internship, full_time, part_time, contract }

    public enum Priority { low, medium, high }

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "resume_id")
    private UUID resumeId;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "job_description")
    private String jobDescription;

    @Column(name = "job_url")
    private String jobUrl;

    @Column
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode")
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type")
    private JobType jobType;

    @Column(name = "salary_min")
    private BigDecimal salaryMin;

    @Column(name = "salary_max")
    private BigDecimal salaryMax;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "salary_currency", length = 3)
    private String salaryCurrency;

    @Column(name = "date_applied", nullable = false)
    private LocalDate dateApplied;

    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "current_stage", nullable = false)
    private ApplicationStatus currentStage;

    @Enumerated(EnumType.STRING)
    @Column
    private Priority priority;

    @Column
    private String source;

    @Column(name = "is_referral", nullable = false)
    private boolean isReferral = false;

    @Column
    private LocalDate deadline;

    @Column(name = "recruiter_name")
    private String recruiterName;

    @Column(name = "recruiter_email")
    private String recruiterEmail;

    @Column
    private String notes;

    @Column(name = "cover_letter")
    private String coverLetter;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "stage_entered_at", nullable = false)
    private Instant stageEnteredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Application() {
        // JPA
    }

    public Application(UUID userId, UUID companyId, String jobTitle, LocalDate dateApplied, ApplicationStatus status) {
        this.userId = userId;
        this.companyId = companyId;
        this.jobTitle = jobTitle;
        this.dateApplied = dateApplied;
        this.status = status;
        this.currentStage = status.isTerminal() ? ApplicationStatus.SAVED : status;
        this.stageEnteredAt = Instant.now();
    }

    public void applyStatusChange(ApplicationStatus newStatus) {
        this.status = newStatus;
        if (!newStatus.isTerminal()) {
            this.currentStage = newStatus;
        }
        this.stageEnteredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public UUID getResumeId() {
        return resumeId;
    }

    public void setResumeId(UUID resumeId) {
        this.resumeId = resumeId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public WorkMode getWorkMode() {
        return workMode;
    }

    public void setWorkMode(WorkMode workMode) {
        this.workMode = workMode;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public BigDecimal getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(BigDecimal salaryMin) {
        this.salaryMin = salaryMin;
    }

    public BigDecimal getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(BigDecimal salaryMax) {
        this.salaryMax = salaryMax;
    }

    public String getSalaryCurrency() {
        return salaryCurrency;
    }

    public void setSalaryCurrency(String salaryCurrency) {
        this.salaryCurrency = salaryCurrency;
    }

    public LocalDate getDateApplied() {
        return dateApplied;
    }

    public void setDateApplied(LocalDate dateApplied) {
        this.dateApplied = dateApplied;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public ApplicationStatus getCurrentStage() {
        return currentStage;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isReferral() {
        return isReferral;
    }

    public void setReferral(boolean referral) {
        isReferral = referral;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getRecruiterName() {
        return recruiterName;
    }

    public void setRecruiterName(String recruiterName) {
        this.recruiterName = recruiterName;
    }

    public String getRecruiterEmail() {
        return recruiterEmail;
    }

    public void setRecruiterEmail(String recruiterEmail) {
        this.recruiterEmail = recruiterEmail;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Instant getStageEnteredAt() {
        return stageEnteredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
