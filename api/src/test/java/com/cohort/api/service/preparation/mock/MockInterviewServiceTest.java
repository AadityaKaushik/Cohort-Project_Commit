package com.cohort.api.service.preparation.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cohort.api.dto.MockInterviewQuestionResponse;
import com.cohort.api.entity.Difficulty;
import com.cohort.api.entity.PracticeAttempt;
import com.cohort.api.entity.Question;
import com.cohort.api.exception.InvalidMockInterviewCountException;
import com.cohort.api.repository.PracticeAttemptRepository;
import com.cohort.api.repository.QuestionRepository;
import com.cohort.api.service.recommendation.TopicMasteryLoader;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockInterviewServiceTest {
    private static final Instant NOW = Instant.parse("2026-03-10T09:00:00Z");

    private final UUID userId = UUID.randomUUID();
    private final PracticeAttemptRepository attempts = mock(PracticeAttemptRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final TopicMasteryLoader masteryLoader = mock(TopicMasteryLoader.class);
    private final MockInterviewService service =
            new MockInterviewService(attempts, questions, masteryLoader, Clock.fixed(NOW, ZoneOffset.UTC));

    private TopicMasterySnapshot topic(UUID id, String name, double mastery, int observations, boolean sufficient) {
        return new TopicMasterySnapshot(id, name, mastery, mastery, observations, sufficient, NOW);
    }

    private Question question(UUID topicId, UUID owner, String text) {
        Question question = mock(Question.class);
        when(question.getId()).thenReturn(UUID.randomUUID());
        when(question.getTopicId()).thenReturn(topicId);
        when(question.getUserId()).thenReturn(owner);
        when(question.isSystem()).thenReturn(owner == null);
        when(question.getQuestionText()).thenReturn(text);
        when(question.getDifficulty()).thenReturn(Difficulty.medium);
        return question;
    }

    private void givenQuestions(UUID topicId, Question... topicQuestions) {
        when(questions.findAllByTopicId(topicId)).thenReturn(List.of(topicQuestions));
    }

    @Test
    void weakestTopicsComeFirstThenNeverPractisedOnes() {
        UUID graphs = UUID.randomUUID();
        UUID heaps = UUID.randomUUID();
        UUID arrays = UUID.randomUUID();
        UUID trees = UUID.randomUUID();
        when(masteryLoader.loadAll(any(), any())).thenReturn(List.of(
                topic(graphs, "Graphs", 0.7, 5, true),
                topic(heaps, "Heaps", 0.3, 4, true),
                topic(trees, "Trees", 0.0, 0, false),
                topic(arrays, "Arrays", 0.0, 0, false)));
        for (UUID id : List.of(graphs, heaps, arrays, trees)) {
            givenQuestions(id, question(id, null, "Question"));
        }
        when(attempts.findAllByUserId(userId)).thenReturn(List.of());

        List<MockInterviewQuestionResponse> result = service.build(userId, 4).questions();

        assertThat(result).extracting(MockInterviewQuestionResponse::topicName)
                .containsExactly("Heaps", "Graphs", "Arrays", "Trees");
        assertThat(result.get(0).reason()).isEqualTo("Mastery 30%");
        assertThat(result.get(0).masteryScore()).isEqualTo(0.3);
        assertThat(result.get(2).reason()).isEqualTo("Not practised yet");
        assertThat(result.get(2).masteryScore()).isNull();
    }

    @Test
    void equalMasteryIsOrderedByTopicName() {
        UUID b = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        when(masteryLoader.loadAll(any(), any())).thenReturn(List.of(
                topic(b, "Bits", 0.5, 3, true), topic(a, "Arrays", 0.5, 3, true)));
        givenQuestions(a, question(a, null, "A"));
        givenQuestions(b, question(b, null, "B"));
        when(attempts.findAllByUserId(userId)).thenReturn(List.of());

        assertThat(service.build(userId, 3).questions())
                .extracting(MockInterviewQuestionResponse::topicName).containsExactly("Arrays", "Bits");
    }

    @Test
    void picksTheLeastRecentlyAttemptedQuestionAndNeverAnotherUsers() {
        UUID topicId = UUID.randomUUID();
        Question old = question(topicId, null, "Older attempt");
        Question recent = question(topicId, null, "Recent attempt");
        Question foreign = question(topicId, UUID.randomUUID(), "Aaa someone else's");
        when(masteryLoader.loadAll(any(), any())).thenReturn(List.of(topic(topicId, "Graphs", 0.4, 3, true)));
        givenQuestions(topicId, recent, old, foreign);
        UUID oldId = old.getId();
        UUID recentId = recent.getId();
        List<PracticeAttempt> history = List.of(
                new PracticeAttempt(userId, oldId, PracticeAttempt.Status.in_progress, Instant.parse("2026-01-01T10:00:00Z")),
                new PracticeAttempt(userId, recentId, PracticeAttempt.Status.in_progress, Instant.parse("2026-03-01T10:00:00Z")));
        when(attempts.findAllByUserId(userId)).thenReturn(history);

        assertThat(service.build(userId, 3).questions())
                .extracting(MockInterviewQuestionResponse::questionId).containsExactly(old.getId());
    }

    @Test
    void neverAttemptedQuestionBeatsAttemptedOneAndTiesGoByText() {
        UUID topicId = UUID.randomUUID();
        Question attempted = question(topicId, null, "Attempted");
        Question fresh = question(topicId, null, "Brand new");
        Question freshEarlier = question(topicId, null, "Another new");
        when(masteryLoader.loadAll(any(), any())).thenReturn(List.of(topic(topicId, "Graphs", 0.4, 3, true)));
        givenQuestions(topicId, attempted, fresh, freshEarlier);
        UUID attemptedId = attempted.getId();
        List<PracticeAttempt> history =
                List.of(new PracticeAttempt(userId, attemptedId, PracticeAttempt.Status.in_progress, NOW));
        when(attempts.findAllByUserId(userId)).thenReturn(history);

        assertThat(service.build(userId, 3).questions())
                .extracting(MockInterviewQuestionResponse::questionId).containsExactly(freshEarlier.getId());
    }

    @Test
    void topicWithNoUsableQuestionIsSkipped() {
        UUID empty = UUID.randomUUID();
        UUID full = UUID.randomUUID();
        when(masteryLoader.loadAll(any(), any())).thenReturn(List.of(
                topic(empty, "Empty", 0.1, 3, true), topic(full, "Full", 0.9, 3, true)));
        givenQuestions(empty, question(empty, UUID.randomUUID(), "Not mine"));
        givenQuestions(full, question(full, null, "Mine"));
        when(attempts.findAllByUserId(userId)).thenReturn(List.of());

        assertThat(service.build(userId, 3).questions())
                .extracting(MockInterviewQuestionResponse::topicName).containsExactly("Full");
    }

    @Test
    void countOutsideTheRangeIsRejected() {
        assertThatThrownBy(() -> service.build(userId, 2)).isInstanceOf(InvalidMockInterviewCountException.class);
        assertThatThrownBy(() -> service.build(userId, 11)).isInstanceOf(InvalidMockInterviewCountException.class);
    }
}
