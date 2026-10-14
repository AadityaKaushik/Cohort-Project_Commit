package com.cohort.api.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(ApiError.of("VALIDATION_ERROR", "One or more fields are invalid.", fieldErrors));
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<ApiError> handleWeakPassword(WeakPasswordException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("WEAK_PASSWORD", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("INVALID_CREDENTIALS", ex.getMessage()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiError> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("INVALID_REFRESH_TOKEN", ex.getMessage()));
    }

    @ExceptionHandler(InvalidAuthTokenException.class)
    public ResponseEntity<ApiError> handleInvalidAuthToken(InvalidAuthTokenException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("INVALID_TOKEN", ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleEmailConflict(EmailAlreadyRegisteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("REGISTRATION_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ApiError> handleEmailInUse(EmailAlreadyInUseException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of("EMAIL_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> handleRateLimit(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiError.of("RATE_LIMITED", ex.getMessage()));
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ApiError> handleApplicationNotFound(ApplicationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("APPLICATION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("INVALID_STATUS_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("VALIDATION_ERROR", "The request body is missing or contains an invalid value."));
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ApiError> handleInvalidFile(InvalidFileException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiError.of("INVALID_FILE", ex.getMessage()));
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<ApiError> handleFileTooLarge(FileTooLargeException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiError.of("FILE_TOO_LARGE", ex.getMessage()));
    }

    @ExceptionHandler(PrivacyLedgerEntryNotFoundException.class)
    public ResponseEntity<ApiError> handlePrivacyLedgerEntryNotFound(PrivacyLedgerEntryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("PRIVACY_LEDGER_ENTRY_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ResumeNotFoundException.class)
    public ResponseEntity<ApiError> handleResumeNotFound(ResumeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("RESUME_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ResumeFileNotFoundException.class)
    public ResponseEntity<ApiError> handleResumeFileNotFound(ResumeFileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("RESUME_FILE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InterviewNotFoundException.class)
    public ResponseEntity<ApiError> handleInterviewNotFound(InterviewNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("INTERVIEW_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InterviewQuestionNotFoundException.class)
    public ResponseEntity<ApiError> handleInterviewQuestionNotFound(InterviewQuestionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("INTERVIEW_QUESTION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InterviewFeedbackNotFoundException.class)
    public ResponseEntity<ApiError> handleInterviewFeedbackNotFound(InterviewFeedbackNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("INTERVIEW_FEEDBACK_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateRoundNumberException.class)
    public ResponseEntity<ApiError> handleDuplicateRoundNumber(DuplicateRoundNumberException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("DUPLICATE_ROUND_NUMBER", ex.getMessage()));
    }

    @ExceptionHandler(TopicNotFoundException.class)
    public ResponseEntity<ApiError> handleTopicNotFound(TopicNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("TOPIC_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<ApiError> handleQuestionNotFound(QuestionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("QUESTION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(PreparationTaskNotFoundException.class)
    public ResponseEntity<ApiError> handlePreparationTaskNotFound(PreparationTaskNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("PREPARATION_TASK_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ContactNotFoundException.class)
    public ResponseEntity<ApiError> handleContactNotFound(ContactNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("CONTACT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(OfferNotFoundException.class)
    public ResponseEntity<ApiError> handleOfferNotFound(OfferNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("OFFER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateOfferException.class)
    public ResponseEntity<ApiError> handleDuplicateOffer(DuplicateOfferException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of("DUPLICATE_OFFER", ex.getMessage()));
    }

    @ExceptionHandler(CompanyNoteNotFoundException.class)
    public ResponseEntity<ApiError> handleCompanyNoteNotFound(CompanyNoteNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("COMPANY_NOTE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ApiError> handleCompanyNotFound(CompanyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("COMPANY_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(RecommendationNotFoundException.class)
    public ResponseEntity<ApiError> handleRecommendationNotFound(RecommendationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("RECOMMENDATION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotificationNotFound(NotificationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("NOTIFICATION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ReportExportNotFoundException.class)
    public ResponseEntity<ApiError> handleReportExportNotFound(ReportExportNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("REPORT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ReportExportExpiredException.class)
    public ResponseEntity<ApiError> handleReportExportExpired(ReportExportExpiredException ex) {
        return ResponseEntity.status(HttpStatus.GONE).body(ApiError.of("REPORT_EXPIRED", ex.getMessage()));
    }

    @ExceptionHandler(InvalidReportRequestException.class)
    public ResponseEntity<ApiError> handleInvalidReportRequest(InvalidReportRequestException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("VALIDATION_ERROR", ex.getMessage(), ex.getFieldErrors()));
    }

    @ExceptionHandler(InvalidAccountRequestException.class)
    public ResponseEntity<ApiError> handleInvalidAccountRequest(InvalidAccountRequestException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("VALIDATION_ERROR", ex.getMessage(), ex.getFieldErrors()));
    }

    // A request whose multipart body exceeds Spring's own servlet-level cap (application.yml), above the 5 MB business rule.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiError.of("FILE_TOO_LARGE", "Uploaded file is too large."));
    }
}
