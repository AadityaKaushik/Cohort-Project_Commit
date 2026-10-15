package com.cohort.api.repository;

import com.cohort.api.dto.SampleDataResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SampleDataRepository {
    public static final String MARKER = "[Sample] ";

    private final JdbcTemplate jdbc;

    public SampleDataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean hasSampleCompanies(UUID userId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM companies WHERE created_by_user_id = ? AND starts_with(name, ?)",
                Integer.class, userId, MARKER);
        return count != null && count > 0;
    }

    public SampleDataResponse counts(UUID userId) {
        int applications = count("SELECT count(*) FROM applications WHERE user_id = ? AND starts_with(job_title, ?)", userId);
        int interviews = count("""
                SELECT count(*) FROM interviews i JOIN applications a ON a.id = i.application_id
                WHERE a.user_id = ? AND starts_with(a.job_title, ?)""", userId);
        int contacts = count("SELECT count(*) FROM contacts WHERE user_id = ? AND starts_with(name, ?)", userId);
        return new SampleDataResponse(applications > 0, applications, interviews, contacts);
    }

    public UUID insertCompany(UUID userId, String name, String website, String industry) {
        UUID id = UUID.randomUUID();
        String fullName = MARKER + name;
        jdbc.update("""
                INSERT INTO companies (id, name, normalized_name, website, industry, created_by_user_id)
                VALUES (?, ?, ?, ?, ?, ?)""", id, fullName, fullName.toLowerCase(), website, industry, userId);
        return id;
    }

    public void insertCompanyNote(UUID userId, UUID companyId, String body) {
        jdbc.update("INSERT INTO company_notes (id, user_id, company_id, body) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), userId, companyId, body);
    }

    public UUID insertApplication(UUID userId, UUID companyId, String jobTitle, String status, String stage,
                                  int daysAgoApplied, String location, String workMode, String source, String notes) {
        UUID id = UUID.randomUUID();
        LocalDate applied = LocalDate.now().minusDays(daysAgoApplied);
        Instant appliedAt = applied.atStartOfDay().toInstant(ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO applications (id, user_id, company_id, job_title, date_applied, status, current_stage,
                    stage_entered_at, location, work_mode, job_type, priority, source, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'internship', 'medium', ?, ?)""",
                id, userId, companyId, MARKER + jobTitle, applied, status, stage,
                OffsetDateTime.ofInstant(appliedAt, ZoneOffset.UTC),
                location, workMode, source, notes);
        insertHistory(id, null, "applied", appliedAt);
        if (!"applied".equals(status)) {
            insertHistory(id, "applied", status, appliedAt.plusSeconds(3600L * 24));
        }
        return id;
    }

    private void insertHistory(UUID applicationId, String from, String to, Instant changedAt) {
        jdbc.update("""
                INSERT INTO application_status_history (id, application_id, from_status, to_status, changed_at)
                VALUES (?, ?, ?, ?, ?)""",
                UUID.randomUUID(), applicationId, from, to, OffsetDateTime.ofInstant(changedAt, ZoneOffset.UTC));
    }

    public UUID insertInterview(UUID userId, UUID applicationId, int round, String roundType, int daysFromNow,
                                boolean completed, String outcome, Integer difficulty, String interviewer) {
        UUID id = UUID.randomUUID();
        OffsetDateTime at = OffsetDateTime.now(ZoneOffset.UTC).plusDays(daysFromNow).withHour(10).withMinute(0).withSecond(0).withNano(0);
        jdbc.update("""
                INSERT INTO interviews (id, application_id, user_id, scheduled_at, completed_at, round_number, round_type,
                    duration_minutes, mode, interviewer_name, interviewer_role, outcome, perceived_difficulty)
                VALUES (?, ?, ?, ?, ?, ?, ?, 45, 'video', ?, 'Engineer', ?, ?)""",
                id, applicationId, userId, at, completed ? at.plusMinutes(45) : null, round, roundType,
                MARKER + interviewer, outcome, difficulty);
        return id;
    }

    public void insertQuestion(UUID userId, UUID interviewId, String text, String category, String difficulty, Integer rating) {
        jdbc.update("""
                INSERT INTO interview_questions (id, interview_id, user_id, question_text, category, difficulty, self_rating)
                VALUES (?, ?, ?, ?, ?, ?, ?)""",
                UUID.randomUUID(), interviewId, userId, text, category, difficulty, rating);
    }

    public void insertContact(UUID userId, UUID companyId, UUID applicationId, String name, String roleTitle,
                              String relationship, String referralStatus, int contactedDaysAgo) {
        jdbc.update("""
                INSERT INTO contacts (id, user_id, company_id, application_id, name, role_title, relationship,
                    referral_status, last_contacted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                UUID.randomUUID(), userId, companyId, applicationId, MARKER + name, roleTitle, relationship,
                referralStatus, OffsetDateTime.now(ZoneOffset.UTC).minusDays(contactedDaysAgo));
    }

    public void insertOffer(UUID userId, UUID applicationId, int baseSalary, int daysToRespond) {
        jdbc.update("""
                INSERT INTO offers (id, user_id, application_id, base_salary, bonus, currency, work_mode, status, response_deadline)
                VALUES (?, ?, ?, ?, 5000, 'USD', 'hybrid', 'pending', ?)""",
                UUID.randomUUID(), userId, applicationId, baseSalary, LocalDate.now().plusDays(daysToRespond));
    }

    public void deleteSample(UUID userId) {
        jdbc.update("""
                DELETE FROM notifications WHERE user_id = ? AND related_entity_id IN (
                    SELECT id FROM applications WHERE user_id = ? AND starts_with(job_title, ?)
                    UNION SELECT i.id FROM interviews i JOIN applications a ON a.id = i.application_id
                          WHERE a.user_id = ? AND starts_with(a.job_title, ?)
                    UNION SELECT id FROM contacts WHERE user_id = ? AND starts_with(name, ?))""",
                userId, userId, MARKER, userId, MARKER, userId, MARKER);
        jdbc.update("DELETE FROM contacts WHERE user_id = ? AND starts_with(name, ?)", userId, MARKER);
        jdbc.update("DELETE FROM applications WHERE user_id = ? AND starts_with(job_title, ?)", userId, MARKER);
        jdbc.update("""
                DELETE FROM companies c WHERE c.created_by_user_id = ? AND starts_with(c.name, ?)
                AND NOT EXISTS (SELECT 1 FROM applications a WHERE a.company_id = c.id)""", userId, MARKER);
    }

    private int count(String sql, UUID userId) {
        Integer value = jdbc.queryForObject(sql, Integer.class, userId, MARKER);
        return value == null ? 0 : value;
    }
}
