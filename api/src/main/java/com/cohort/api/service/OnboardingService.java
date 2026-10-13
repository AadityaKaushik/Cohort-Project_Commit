package com.cohort.api.service;

import com.cohort.api.dto.OnboardingRequest;
import com.cohort.api.dto.OnboardingResponse;
import com.cohort.api.dto.UserProfileResponse;
import com.cohort.api.entity.PreparationTask;
import com.cohort.api.entity.Topic;
import com.cohort.api.entity.User;
import com.cohort.api.repository.PreparationTaskRepository;
import com.cohort.api.repository.TopicRepository;
import com.cohort.api.repository.UserRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-91: seeds a starter preparation plan from the foundational topics, spread across the user's runway. */
@Service
public class OnboardingService {

    static final int MAX_STARTER_TASKS = 6;
    private static final int DEFAULT_RUNWAY_DAYS = 42;
    private static final int STARTER_EFFORT_MINUTES = 60;

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final PreparationTaskRepository taskRepository;

    public OnboardingService(
            UserRepository userRepository, TopicRepository topicRepository, PreparationTaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.topicRepository = topicRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public OnboardingResponse complete(UUID userId, OnboardingRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalStateException("Authenticated user " + userId + " no longer exists"));
        if (user.getOnboardedAt() != null) {
            return new OnboardingResponse(UserProfileResponse.from(user), 0);
        }

        if (!request.skip()) {
            if (request.targetRole() != null && !request.targetRole().isBlank()) {
                user.setTargetRole(request.targetRole().trim());
            }
            user.setTargetDate(request.targetDate());
        }
        user.markOnboarded();
        userRepository.save(user);

        int created = request.skip() ? 0 : seedStarterPlan(userId, request.targetDate());
        return new OnboardingResponse(UserProfileResponse.from(user), created);
    }

    private int seedStarterPlan(UUID userId, LocalDate targetDate) {
        List<Topic> foundational = foundationalTopicsRoundRobin();
        LocalDate today = LocalDate.now();
        long runway = targetDate == null ? DEFAULT_RUNWAY_DAYS : Math.max(1, ChronoUnit.DAYS.between(today, targetDate));
        int count = foundational.size();
        for (int index = 0; index < count; index++) {
            Topic topic = foundational.get(index);
            PreparationTask task = new PreparationTask(userId, topic.getId(), "Review " + topic.getName());
            task.setEstimatedEffortMinutes(STARTER_EFFORT_MINUTES);
            task.setDueDate(today.plusDays(Math.max(1, runway * (index + 1) / count)));
            taskRepository.save(task);
        }
        return count;
    }

    private List<Topic> foundationalTopicsRoundRobin() {
        Set<UUID> hasPrerequisite = new HashSet<>();
        topicRepository.findAllPrerequisiteEdges().forEach(edge -> hasPrerequisite.add(edge.getTopicId()));

        Map<Topic.Category, Queue<Topic>> byCategory = new EnumMap<>(Topic.Category.class);
        for (Topic topic : topicRepository.findAllByOrderByCategoryAscNameAsc()) {
            if (!hasPrerequisite.contains(topic.getId())) {
                byCategory.computeIfAbsent(topic.getCategory(), key -> new LinkedList<>()).add(topic);
            }
        }

        List<Topic> picked = new ArrayList<>();
        while (picked.size() < MAX_STARTER_TASKS && byCategory.values().stream().anyMatch(queue -> !queue.isEmpty())) {
            for (Queue<Topic> queue : byCategory.values()) {
                if (picked.size() < MAX_STARTER_TASKS && !queue.isEmpty()) {
                    picked.add(queue.poll());
                }
            }
        }
        return picked;
    }
}
