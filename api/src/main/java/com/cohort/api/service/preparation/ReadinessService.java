package com.cohort.api.service.preparation;

import com.cohort.api.dto.ReadinessResponse;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.Topic;
import com.cohort.api.exception.ApplicationNotFoundException;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.TopicRepository;
import com.cohort.api.service.preparation.ReadinessMetric.WeightedTopic;
import com.cohort.api.service.preparation.analytics.AnalyticsResult;
import com.cohort.api.service.recommendation.TopicMasteryLoader;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadinessService {
    private final ApplicationRepository applicationRepository;
    private final TopicRepository topicRepository;
    private final TopicMasteryLoader masteryLoader;
    private final JobSkillMapper skillMapper;
    private final ReadinessMetric metric = new ReadinessMetric();

    public ReadinessService(
            ApplicationRepository applicationRepository,
            TopicRepository topicRepository,
            TopicMasteryLoader masteryLoader,
            JobSkillMapper skillMapper) {
        this.applicationRepository = applicationRepository;
        this.topicRepository = topicRepository;
        this.masteryLoader = masteryLoader;
        this.skillMapper = skillMapper;
    }

    @Transactional(readOnly = true)
    public ReadinessResponse forApplication(UUID userId, UUID applicationId) {
        Application application = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(ApplicationNotFoundException::new);

        String text = application.getJobTitle() + " " + (application.getJobDescription() == null ? "" : application.getJobDescription());
        Map<Topic, Integer> mentions = skillMapper.mentions(text, topicRepository.findAllByOrderByCategoryAscNameAsc());

        Map<UUID, TopicMasterySnapshot> masteryByTopic = new HashMap<>();
        for (TopicMasterySnapshot snapshot : masteryLoader.loadAll(userId, Instant.now())) {
            masteryByTopic.put(snapshot.topicId(), snapshot);
        }

        List<WeightedTopic> weighted = mentions.entrySet().stream()
                .map(entry -> {
                    TopicMasterySnapshot snapshot = masteryByTopic.get(entry.getKey().getId());
                    return new WeightedTopic(
                            entry.getKey().getId(), entry.getKey().getName(), entry.getValue(),
                            snapshot.masteryScore(), snapshot.sufficientEvidence());
                })
                .toList();

        AnalyticsResult<Double> result = metric.evaluate(new ReadinessMetric.Input(weighted));
        List<ReadinessResponse.TopicContribution> topics = weighted.stream()
                .map(t -> new ReadinessResponse.TopicContribution(t.topicId(), t.topicName(), t.weight(), t.masteryScore(), t.hasEvidence()))
                .toList();
        Double score = result.sufficientData() ? result.value() : null;
        return new ReadinessResponse(score, result.formatted(), result.sampleSize(), result.sufficientData(), topics);
    }
}
