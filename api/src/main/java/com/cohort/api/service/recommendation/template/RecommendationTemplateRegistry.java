package com.cohort.api.service.recommendation.template;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

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
}
