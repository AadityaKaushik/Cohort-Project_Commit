package com.cohort.api.service.recommendation.template;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * US-53/54 / PROJECT_PLAN.md §21.7: "All user-facing recommendation copy
 * lives in one registry module." Collects every {@link RecommendationTemplate}
 * Spring finds (one per {@link RecommendationType}) and dispatches a
 * candidate to the matching one -- {@code RecommendationFacade} (P42) never
 * needs an {@code if/switch} over recommendation type to find its copy.
 */
@Component
public class RecommendationTemplateRegistry {

    private final Map<RecommendationType, RecommendationTemplate> templatesByType;

    public RecommendationTemplateRegistry(List<RecommendationTemplate> templates) {
        this.templatesByType = templates.stream().collect(Collectors.toMap(RecommendationTemplate::type, Function.identity()));
    }

    public RenderedRecommendationCopy render(RecommendationCandidate candidate, EvidenceTier tier) {
        RecommendationTemplate template = templatesByType.get(candidate.type());
        if (template == null) {
            throw new IllegalStateException("No recommendation template registered for type " + candidate.type());
        }
        return template.render(candidate, tier);
    }

    /** Every registered type, for tests that need to iterate the whole registry (e.g. {@code test_no_causal_language}). */
    public List<RecommendationTemplate> allTemplates() {
        return List.copyOf(templatesByType.values());
    }
}
