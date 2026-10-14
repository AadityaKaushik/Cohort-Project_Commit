package com.cohort.api.repository.specification;

import com.cohort.api.entity.Difficulty;
import com.cohort.api.entity.Question;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class QuestionSpecifications {
    private QuestionSpecifications() {
    }

    public static Specification<Question> visibleTo(UUID userId) {
        return (root, query, cb) ->
                cb.or(cb.isTrue(root.get("isSystem")), cb.equal(root.get("userId"), userId));
    }

    public static Specification<Question> hasCategory(Question.Category category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Question> hasTopic(UUID topicId) {
        return (root, query, cb) -> topicId == null ? null : cb.equal(root.get("topicId"), topicId);
    }

    public static Specification<Question> hasDifficulty(Difficulty difficulty) {
        return (root, query, cb) -> difficulty == null ? null : cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Question> isSystem(Boolean isSystem) {
        return (root, query, cb) -> isSystem == null ? null : cb.equal(root.get("isSystem"), isSystem);
    }

    public static Specification<Question> textMatches(String q) {
        return (root, query, cb) ->
                q == null || q.isBlank()
                        ? null
                        : cb.like(cb.lower(root.get("questionText")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<Question> combine(
            UUID userId, Question.Category category, UUID topicId, Difficulty difficulty, Boolean isSystem, String q) {
        return visibleTo(userId)
                .and(hasCategory(category))
                .and(hasTopic(topicId))
                .and(hasDifficulty(difficulty))
                .and(isSystem(isSystem))
                .and(textMatches(q));
    }
}
