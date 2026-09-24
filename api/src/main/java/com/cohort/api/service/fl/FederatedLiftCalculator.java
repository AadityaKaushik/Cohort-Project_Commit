package com.cohort.api.service.fl;

import org.springframework.stereotype.Component;

/**
 * US-65/US-77 (Federated Lift viewer): turns the current global model into
 * a per-candidate score multiplier — the "federated signal" the viewer
 * lets a user compare against local-only ranking.
 *
 * <p><b>The one property this class exists to guarantee, and the one P57's
 * test is built around:</b> the multiplier is always clamped to
 * {@code [MIN_MULTIPLIER, MAX_MULTIPLIER]} — a sanity clamp so the
 * federated signal can only nudge a candidate's rank by plus-or-minus 25%,
 * never dominate the local, rule-based score it is layered on top of. This
 * is deliberate and load-bearing: US-65's whole premise (§21's "rules
 * generate candidates, the learned model only re-ranks them") only holds
 * if the model's influence is bounded, never absolute.
 *
 * <p>The multiplier is a real function of the fetched {@link FlModelInfo}'s
 * parameters (not a fixed constant), and varies per candidate via {@code
 * candidateSeverity} (already computed per-candidate by the generator
 * chain, P39) — so different candidates for the same user can be nudged
 * differently, which is what makes a rank delta meaningful. It is not
 * claimed to be a trained, validated inference (that would require the
 * model's actual training objective and the user's full feature vector at
 * inference time, out of scope for this batch); it is an honest,
 * deterministic, bounded function of real model output.
 */
@Component
public class FederatedLiftCalculator {

    public static final double MIN_MULTIPLIER = 0.75;
    public static final double MAX_MULTIPLIER = 1.25;

    /** {@code candidateSeverity}: a candidate's own [0,1] severity (see {@code RecommendationCandidate#severity()}). */
    public double adjustmentMultiplier(FlModelInfo model, double candidateSeverity) {
        double meanParameter = model.parameters().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double raw = meanParameter * (candidateSeverity - 0.5);
        if (!Double.isFinite(raw)) {
            // A NaN would slip straight through Math.max/min, and an infinity is
            // not a signal worth trusting either: treat both as "no adjustment."
            return 1.0;
        }
        return clamp(1.0 + raw, MIN_MULTIPLIER, MAX_MULTIPLIER);
    }

    private double clamp(double value, double lo, double hi) {
        return Math.max(lo, Math.min(hi, value));
    }
}
