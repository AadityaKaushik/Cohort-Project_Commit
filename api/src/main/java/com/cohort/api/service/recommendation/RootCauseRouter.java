package com.cohort.api.service.recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RootCauseRouter {
    static final int MAX_DEPTH = 2;
    static final double MARGIN = 0.15;

    public record RoutingResult(UUID topicId, List<UUID> chain) {
    }

    public RoutingResult routeToRootCause(UUID topicId, Map<UUID, Double> masteryByTopicId, Map<UUID, List<UUID>> prerequisitesByTopic) {
        List<UUID> chain = new ArrayList<>();
        chain.add(topicId);
        return route(topicId, masteryByTopicId, prerequisitesByTopic, 0, chain);
    }

    private RoutingResult route(
            UUID topicId, Map<UUID, Double> masteryByTopicId, Map<UUID, List<UUID>> prerequisitesByTopic, int depth, List<UUID> chainSoFar) {
        if (depth >= MAX_DEPTH) {
            return new RoutingResult(topicId, chainSoFar);
        }

        UUID weakestPrerequisite = argminMastery(prerequisitesByTopic.getOrDefault(topicId, List.of()), masteryByTopicId);
        if (weakestPrerequisite == null) {
            return new RoutingResult(topicId, chainSoFar);
        }

        double currentMastery = masteryByTopicId.getOrDefault(topicId, 0.0);
        double prerequisiteMastery = masteryByTopicId.getOrDefault(weakestPrerequisite, 1.0);
        if (prerequisiteMastery < currentMastery - MARGIN) {
            List<UUID> nextChain = new ArrayList<>(chainSoFar);
            nextChain.add(weakestPrerequisite);
            return route(weakestPrerequisite, masteryByTopicId, prerequisitesByTopic, depth + 1, nextChain);
        }
        return new RoutingResult(topicId, chainSoFar);
    }

    private UUID argminMastery(List<UUID> prerequisiteIds, Map<UUID, Double> masteryByTopicId) {
        UUID weakest = null;
        double weakestMastery = Double.POSITIVE_INFINITY;
        for (UUID prerequisiteId : prerequisiteIds) {
            double mastery = masteryByTopicId.getOrDefault(prerequisiteId, 1.0);
            if (mastery < weakestMastery) {
                weakestMastery = mastery;
                weakest = prerequisiteId;
            }
        }
        return weakest;
    }
}
