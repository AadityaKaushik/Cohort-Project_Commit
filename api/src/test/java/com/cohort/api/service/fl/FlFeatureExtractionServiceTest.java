package com.cohort.api.service.fl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.PracticeAttempt;
import com.cohort.api.entity.Resume;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.InterviewFeedbackRepository;
import com.cohort.api.repository.InterviewRepository;
import com.cohort.api.repository.PracticeAttemptRepository;
import com.cohort.api.repository.PreparationTaskRepository;
import com.cohort.api.repository.QuestionRepository;
import com.cohort.api.repository.ResumeRepository;
import com.cohort.api.repository.TopicRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlFeatureExtractionServiceTest {
    @Mock
    private PracticeAttemptRepository practiceAttemptRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private PreparationTaskRepository preparationTaskRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private InterviewFeedbackRepository interviewFeedbackRepository;

    private FlFeatureExtractionService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new FlFeatureExtractionService(
                practiceAttemptRepository, questionRepository, topicRepository, preparationTaskRepository,
                applicationRepository, resumeRepository, interviewRepository, interviewFeedbackRepository);
        userId = UUID.randomUUID();
        when(topicRepository.count()).thenReturn(40L);
        // lenient: extract() only calls this when there is a practice attempt.
        org.mockito.Mockito.lenient()
                .when(questionRepository.findAllById(org.mockito.ArgumentMatchers.<UUID>anySet()))
                .thenReturn(List.of());
        when(preparationTaskRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(interviewRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(interviewFeedbackRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(resumeRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(applicationRepository.findAllByUserId(userId)).thenReturn(List.of());
    }

    @Test
    void noPostAsOfPracticeAttemptLeaksIntoTheVector() {
        Instant asOf = Instant.parse("2026-01-15T00:00:00Z");
        PracticeAttempt before1 = attempt(asOf.minus(1, ChronoUnit.DAYS));
        PracticeAttempt before2 = attempt(asOf.minusSeconds(1)); // before the cutoff: counted
        PracticeAttempt exactlyAtCutoff = attempt(asOf);
        PracticeAttempt after = attempt(asOf.plusSeconds(1)); // after the cutoff: ignored
        when(practiceAttemptRepository.findAllByUserId(userId)).thenReturn(List.of(before1, before2, exactlyAtCutoff, after));

        FlFeatureVector vector = service.extract(userId, asOf);

        double expectedForThreeAttempts = Math.log1p(3) / Math.log1p(500);
        assertThat(vector.practiceVolume()).isCloseTo(expectedForThreeAttempts, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void everyFeatureStaysWithinItsDeclaredBounds() {
        Instant asOf = Instant.now();
        when(practiceAttemptRepository.findAllByUserId(userId)).thenReturn(List.of(attempt(asOf.minusSeconds(10))));
        when(applicationRepository.findAllByUserId(userId)).thenReturn(List.of(application(asOf.minusSeconds(10))));
        when(resumeRepository.findAllByUserId(userId)).thenReturn(List.of(resume(asOf.minusSeconds(10))));

        FlFeatureVector vector = service.extract(userId, asOf);

        vector.toNamedMap().forEach((name, value) -> {
            double[] bounds = FlFeatureSchema.FEATURE_BOUNDS.get(name);
            assertThat(value).as("feature %s = %s must be within [%s, %s]", name, value, bounds[0], bounds[1])
                    .isBetween(bounds[0] - 1e-9, bounds[1] + 1e-9);
        });
    }

    @Test
    void aUserWithNoDataAtAllProducesAllZeroFeatures() {
        Instant asOf = Instant.now();

        FlFeatureVector vector = service.extract(userId, asOf);

        vector.toNamedMap().values().forEach(value -> assertThat(value).isEqualTo(0.0));
    }

    @Test
    void eachRatedDebriefBecomesOneExampleLabelledByItsRatingAndBuiltFromEarlierDataOnly() {
        Instant firstRound = Instant.parse("2026-02-01T10:00:00Z");
        Instant secondRound = Instant.parse("2026-03-01T10:00:00Z");
        com.cohort.api.entity.Interview first = interview(firstRound);
        com.cohort.api.entity.Interview second = interview(secondRound);
        com.cohort.api.entity.Interview unrated = interview(secondRound.plusSeconds(86400));
        when(interviewRepository.findAllByUserId(userId)).thenReturn(List.of(first, second, unrated));
        when(interviewFeedbackRepository.findAllByUserId(userId)).thenReturn(List.of(
                feedback(first, (short) 5, firstRound.plusSeconds(3600)),
                feedback(second, (short) 2, secondRound.plusSeconds(3600)),
                feedback(unrated, null, secondRound.plusSeconds(90000))));
        when(practiceAttemptRepository.findAllByUserId(userId)).thenReturn(List.of(
                attempt(firstRound.minus(2, ChronoUnit.DAYS)),
                attempt(secondRound.minus(2, ChronoUnit.DAYS))));

        List<FlTrainingExample> examples = service.trainingExamples(userId);

        assertThat(examples).extracting(FlTrainingExample::confident).containsExactly(true, false);
        assertThat(examples.get(0).features().practiceVolume()).isCloseTo(Math.log1p(1) / Math.log1p(500), org.assertj.core.data.Offset.offset(1e-9));
        assertThat(examples.get(0).features().recentSelfRating()).isEqualTo(0.0);
        assertThat(examples.get(1).features().practiceVolume()).isCloseTo(Math.log1p(2) / Math.log1p(500), org.assertj.core.data.Offset.offset(1e-9));
        assertThat(examples.get(1).features().recentSelfRating()).isEqualTo(1.0);
    }

    private com.cohort.api.entity.Interview interview(Instant scheduledAt) {
        com.cohort.api.entity.Interview interview = new com.cohort.api.entity.Interview(
                UUID.randomUUID(), userId, (short) 1, com.cohort.api.entity.InterviewRoundType.values()[0]);
        interview.setScheduledAt(scheduledAt);
        setField(interview, "id", UUID.randomUUID());
        return interview;
    }

    private com.cohort.api.entity.InterviewFeedback feedback(com.cohort.api.entity.Interview interview, Short rating, Instant submittedAt) {
        com.cohort.api.entity.InterviewFeedback feedback = new com.cohort.api.entity.InterviewFeedback(interview.getId(), userId);
        feedback.setSelfRating(rating);
        feedback.setSubmittedAt(submittedAt);
        return feedback;
    }

    private void setField(Object entity, String name, Object value) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private PracticeAttempt attempt(Instant attemptedAt) {
        PracticeAttempt attempt = new PracticeAttempt(userId, UUID.randomUUID(), PracticeAttempt.Status.confident, attemptedAt);
        attempt.setConfidence((short) 7);
        return attempt;
    }

    private Application application(Instant createdAt) {
        Application application = new Application(
                userId, UUID.randomUUID(), "Software Engineer", java.time.LocalDate.now(), com.cohort.api.entity.ApplicationStatus.APPLIED);
        setCreatedAt(application, createdAt);
        return application;
    }

    private Resume resume(Instant createdAt) {
        Resume resume = new Resume(userId, "Primary");
        setCreatedAt(resume, createdAt);
        return resume;
    }

    // createdAt is normally set by Hibernate on persist, so set it by reflection.
    private void setCreatedAt(Object entity, Instant createdAt) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, createdAt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
