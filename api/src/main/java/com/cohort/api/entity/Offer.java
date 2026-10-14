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
@Table(name = "offers")
public class Offer {

    public enum WorkMode { onsite, hybrid, remote }

    public enum Status { pending, negotiating, accepted, declined }

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "base_salary", nullable = false)
    private BigDecimal baseSalary;

    private BigDecimal bonus;

    @Column(name = "equity_value")
    private BigDecimal equityValue;

    @Column(name = "signing_bonus")
    private BigDecimal signingBonus;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency = "USD";

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode")
    private WorkMode workMode;

    private String benefits;

    private String notes;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "response_deadline")
    private LocalDate responseDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.pending;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Offer() {
    }

    public Offer(UUID userId, UUID applicationId, BigDecimal baseSalary) {
        this.userId = userId;
        this.applicationId = applicationId;
        this.baseSalary = baseSalary;
    }

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }

    public UUID getApplicationId() { return applicationId; }

    public BigDecimal getBaseSalary() { return baseSalary; }

    public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }

    public BigDecimal getBonus() { return bonus; }

    public void setBonus(BigDecimal bonus) { this.bonus = bonus; }

    public BigDecimal getEquityValue() { return equityValue; }

    public void setEquityValue(BigDecimal equityValue) { this.equityValue = equityValue; }

    public BigDecimal getSigningBonus() { return signingBonus; }

    public void setSigningBonus(BigDecimal signingBonus) { this.signingBonus = signingBonus; }

    public String getCurrency() { return currency; }

    public void setCurrency(String currency) { this.currency = currency; }

    public String getLocation() { return location; }

    public void setLocation(String location) { this.location = location; }

    public WorkMode getWorkMode() { return workMode; }

    public void setWorkMode(WorkMode workMode) { this.workMode = workMode; }

    public String getBenefits() { return benefits; }

    public void setBenefits(String benefits) { this.benefits = benefits; }

    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getStartDate() { return startDate; }

    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getResponseDeadline() { return responseDeadline; }

    public void setResponseDeadline(LocalDate responseDeadline) { this.responseDeadline = responseDeadline; }

    public Status getStatus() { return status; }

    public void setStatus(Status status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
