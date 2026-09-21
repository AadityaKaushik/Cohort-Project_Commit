package com.cohort.api.service.recommendation.tiering;

/**
 * FR-73 / PROJECT_PLAN.md §21.6's evidence tiers, each carrying the exact
 * {@code tier_weight} the scoring formula uses:
 * {@code score = severity x recency x actionability x tier_weight x federated_adjustment}.
 *
 * <table>
 *   <caption>Tier meanings (PROJECT_PLAN.md, table above §21.6)</caption>
 *   <tr><td>A</td><td>Your data</td><td>&gt;=5 of the user's own observations</td></tr>
 *   <tr><td>B</td><td>Your data, thin</td><td>2-4 observations</td></tr>
 *   <tr><td>C</td><td>Federated</td><td>global model, cohort &gt;= k -- unreachable until P52+ wires a federated signal in</td></tr>
 *   <tr><td>D</td><td>Heuristic</td><td>rules, no data yet</td></tr>
 * </table>
 */
public enum EvidenceTier {
    A(1.0),
    B(0.7),
    C(0.6),
    D(0.4);

    private final double weight;

    EvidenceTier(double weight) {
        this.weight = weight;
    }

    public double weight() {
        return weight;
    }
}
