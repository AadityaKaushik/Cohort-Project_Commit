package com.cohort.api.service.account;

import com.cohort.api.dto.ChangePasswordRequest;
import com.cohort.api.dto.DeleteAccountRequest;
import com.cohort.api.entity.ReportExport;
import com.cohort.api.entity.Resume;
import com.cohort.api.entity.User;
import com.cohort.api.exception.InvalidAccountRequestException;
import com.cohort.api.repository.RefreshTokenRepository;
import com.cohort.api.repository.ReportExportRepository;
import com.cohort.api.repository.ResumeRepository;
import com.cohort.api.repository.UserRepository;
import com.cohort.api.security.PasswordPolicyValidator;
import com.cohort.api.service.fl.FlExportClient;
import com.cohort.api.service.resume.storage.FileStorageStrategy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    static final String DELETE_CONFIRMATION = "DELETE";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ResumeRepository resumeRepository;
    private final ReportExportRepository reportExportRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final FileStorageStrategy resumeStorage;
    private final FileStorageStrategy reportStorage;
    private final FlExportClient flExportClient;

    public AccountService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            ResumeRepository resumeRepository,
            ReportExportRepository reportExportRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            FileStorageStrategy resumeStorage,
            @Qualifier("reportFileStorage") FileStorageStrategy reportStorage,
            FlExportClient flExportClient) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.resumeRepository = resumeRepository;
        this.reportExportRepository = reportExportRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.resumeStorage = resumeStorage;
        this.reportStorage = reportStorage;
        this.flExportClient = flExportClient;
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidAccountRequestException("current_password", "The current password is incorrect.");
        }
        passwordPolicyValidator.validate(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    @Transactional
    public void deleteAccount(UUID userId, DeleteAccountRequest request) {
        if (!DELETE_CONFIRMATION.equals(request.confirmation())) {
            throw new InvalidAccountRequestException("confirmation", "Type " + DELETE_CONFIRMATION + " to confirm.");
        }
        User user = requireUser(userId);

        List<Resume> resumes = resumeRepository.findAllByUserId(userId);
        List<ReportExport> reports = reportExportRepository.findAllByUserId(userId);
        userRepository.delete(user);
        userRepository.flush();

        resumes.stream().filter(Resume::hasFile).forEach(resume -> deleteQuietly(resumeStorage, resume.getFilePath()));
        reports.forEach(report -> deleteQuietly(reportStorage, report.getFilePath()));
        forgetFlClientState(userId);
    }

    private void forgetFlClientState(UUID userId) {
        try {
            flExportClient.delete(userId);
        } catch (RuntimeException ex) {
            log.warn("Could not clear federated-learning client state for deleted user {}: {}", userId, ex.getMessage());
        }
    }

    private void deleteQuietly(FileStorageStrategy storage, String storageKey) {
        try {
            storage.delete(storageKey);
        } catch (RuntimeException ex) {
            log.warn("Could not delete stored file {} for a deleted account: {}", storageKey, ex.getMessage());
        }
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user " + userId + " no longer exists"));
    }
}
