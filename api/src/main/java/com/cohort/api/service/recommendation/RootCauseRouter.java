package com.cohort.api.service.recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * US-52 / PROJECT_PLAN.md §21.5's {@code routeToRootCause} algorithm,
 * implemented exactly as documented there:
 *
 * <pre>
 * routeToRootCause(topic t, mastery M, graph G, depth d = 0):
 *     if d >= 2: return t
 *     weakest_prereq = argmin{ M[p] : p in G.prerequisites(t) }
 *     if weakest_prereq exists and M[weakest_prereq] < M[t] - 0.15:
 *         return routeToRootCause(weakest_prereq, M, G, d+1)
 *     return t
 * </pre>
 *
 * <p>The 0.15 margin prevents routing on noise; the depth cap of 2 keeps the
 * rendered chain comprehensible even on a long or adversarial prerequisite
 * graph. Note that a genuine cycle cannot make this recurse indefinitely
 * even without the cap -- each hop requires strictly lower mastery by the
 * margin, and scores are bounded, so a real graph runs out of room to keep
 * decreasing within a handful of hops regardless. The cap exists primarily
 * for comprehensibility (§21.5: "to stay comprehensible") and, defensively,
 * to bound recursion even if a caller ever hands this a malformed or
 * artificially long chain -- see {@code RootCauseRouterTest}'s pathological
 * graph case.
 */
@Component
public class RootCauseRouter {

    static final int MAX_DEPTH = 2;
    static final double MARGIN = 0.15;

    /** The routed-to topic and the full chain of topic ids walked to reach it, starting with the original topic. */
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
