package com.cohort.api.service.recommendation.generator;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Chain of Responsibility pattern (ARCHITECTURE_REVISION_JAVA.md §4.1): base
 * stage of the recommendation candidate-generation pipeline (US-51,56;
 * PROJECT_PLAN.md §21.2/§21.3). Each concrete generator ({@link WeakTopicGenerator},
 * {@link DecayGenerator}, {@link StalledApplicationGenerator},
 * {@link BaselineGenerator}) inspects the same {@link RecommendationContext}
 * and independently contributes zero or more candidates, then
 * unconditionally forwards the accumulated list to the next link.
 *
 * <p>This deliberately differs from the resume-upload chain
 * ({@code UploadValidationHandler}, US-21), where a failing stage stops the
 * chain by throwing: there, every stage must pass or the whole chain fails;
 * here, "this generator found nothing" is not an error, just an empty
 * contribution, so every stage always runs and always forwards. Both are
 * still Chain of Responsibility -- a request (the growing candidate list)
 * travels down an ordered chain of independent handlers -- they simply
 * differ in what "handling" means for their domain, the same way a Servlet
 * filter chain (always forwards) and an event-bubbling handler chain
 * (first match wins) are both textbook CoR despite behaving differently.
 *
 * <p>The point of {@link #handle} being {@code final} and {@link #generate}
 * being the only extension point is what makes each generator independently
 * unit-testable and the chain itself freely reorderable: a test calls
 * {@code new WeakTopicGenerator().handle(context, List.of())} directly, with
 * no Spring context, no other generator, and no chain-wiring code in the
 * generator itself to get wrong.
 */
public abstract class RecommendationGenerator {

    private RecommendationGenerator next;

    /** Links {@code next} after this generator and returns it, so calls chain fluently: {@code a.linkTo(b).linkTo(c)}. */
    public RecommendationGenerator linkTo(RecommendationGenerator next) {
        this.next = next;
        return next;
    }

    /** Runs this stage's contribution, appends it to what came before, then forwards to the next stage (if any). */
    public final List<RecommendationCandidate> handle(RecommendationContext context, List<RecommendationCandidate> candidatesSoFar) {
        List<RecommendationCandidate> combined = new ArrayList<>(candidatesSoFar);
        combined.addAll(generate(context));
        return next == null ? combined : next.handle(context, combined);
    }

    /** The one thing each concrete generator supplies: its own candidates, computed purely from {@code context}. */
    protected abstract List<RecommendationCandidate> generate(RecommendationContext context);
}
