package com.cohort.api.service.preparation.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cohort.api.dto.ReviewItemResponse;
import com.cohort.api.entity.PracticeAttempt;
import com.cohort.api.entity.Question;
import com.cohort.api.repository.PracticeAttemptRepository;
import com.cohort.api.repository.QuestionRepository;
import com.cohort.api.service.recommendation.TopicMasteryLoader;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewQueueServiceTest {
    private static final Instant NOW = Instant.parse("2026-03-10T09:00:00Z");

    private final UUID userId = UUID.randomUUID();
    private final UUID topicId = UUID.randomUUID();
    private final PracticeAttemptRepository attempts = mock(PracticeAttemptRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final TopicMasteryLoader masteryLoader = mock(TopicMasteryLoader.class);
    private final ReviewQueueService service = new ReviewQueueService(
            attempts, questions, masteryLoader, new ReviewScheduler(), Clock.fixed(NOW, ZoneOffset.UTC));

    private Question question(UUID id, UUID owner, String text) {
        Question question = mock(Question.class);
        when(question.getId()).thenReturn(id);
        when(question.getTopicId()).thenReturn(topicId);
        when(question.getUserId()).thenReturn(owner);
        when(question.isSystem()).thenReturn(owner == null);
        when(question.getQuestionText()).thenReturn(text);
        return question;
    }

    private PracticeAttempt attempt(UUID questionId, String at, int confidence) {
        PracticeAttempt attempt = new PracticeAttempt(
                userId, questionId, PracticeAttempt.Status.in_progress, Instant.parse(at));
        attempt.setConfidence((short) confidence);
        return attempt;
    }

    private void givenMastery(double score, boolean sufficient) {
        when(masteryLoader.loadAll(any(), any())).thenReturn(List.of(
                new TopicMasterySnapshot(topicId, "Graphs", score, score, 2, sufficient, NOW)));
    }

    private void givenHistory(Question question, PracticeAttempt... history) {
        when(attempts.findAllByUserId(userId)).thenReturn(List.of(history));
        when(questions.findAllById(any())).thenReturn(List.of(question));
        when(questions.findAllByTopicId(topicId)).thenReturn(List.of(question));
    }

    @Test
    void topicSixDaysAfterAMissIsDueWithAClearReason() {
        UUID questionId = UUID.randomUUID();
        givenHistory(question(questionId, null, "Detect a cycle"), attempt(questionId, "2026-03-04T10:00:00Z", 2));
        givenMastery(0.4, true);

        List<ReviewItemResponse> items = service.queue(userId).items();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).reason()).isEqualTo("Last practised 6 days ago, mastery 40%");
        assertThat(items.get(0).dueDate()).isEqualTo(LocalDate.parse("2026-03-05"));
        assertThat(items.get(0).daysOverdue()).isEqualTo(5);
        assertThat(items.get(0).questionId()).isEqualTo(questionId);
    }

    @Test
    void topicNotYetDueIsLeftOut() {
        UUID questionId = UUID.randomUUID();
        givenHistory(question(questionId, null, "Detect a cycle"), attempt(questionId, "2026-03-09T10:00:00Z", 4));
        givenMastery(0.8, true);

        assertThat(service.queue(userId).items()).isEmpty();
    }

    @Test
    void masteryIsOmittedFromTheReasonWithoutEnoughEvidence() {
        UUID questionId = UUID.randomUUID();
        givenHistory(question(questionId, null, "Detect a cycle"), attempt(questionId, "2026-03-01T10:00:00Z", 5));
        givenMastery(0.9, false);

        ReviewItemResponse item = service.queue(userId).items().get(0);

        assertThat(item.reason()).isEqualTo("Last practised 9 days ago");
        assertThat(item.masteryScore()).isNull();
    }

    @Test
    void userWithNoAttemptsGetsAnEmptyQueue() {
        when(attempts.findAllByUserId(userId)).thenReturn(List.of());

        assertThat(service.queue(userId).items()).isEmpty();
    }

    @Test
    void anotherUsersQuestionIsNeverOffered() {
        UUID questionId = UUID.randomUUID();
        givenHistory(question(questionId, UUID.randomUUID(), "Someone else's"), attempt(questionId, "2026-03-01T10:00:00Z", 1));
        givenMastery(0.2, true);

        assertThat(service.queue(userId).items()).isEmpty();
    }
}
