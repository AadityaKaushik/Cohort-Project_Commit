package com.cohort.api.service.preparation.analytics;

/**
 * Template Method pattern (ARCHITECTURE_REVISION_JAVA.md §4.1, called out
 * by name in BUILD_PHASES_JAVA.md's own "quick reference" as one of the
 * commits worth walking an evaluator through personally): a fixed skeleton
 * of "compute -&gt; check sample size -&gt; attach {@code sufficient_data} flag
 * -&gt; format" is defined once, here, as a {@code final} method. Every
 * concrete metric (funnel stage attainment, conversion rate, dwell time —
 * built in later Sprint 3 phases on top of this base) supplies only the
 * formula-specific steps: how big is the sample ({@link #sampleSize}) and
 * what is the raw value ({@link #compute}), plus an optional display-only
 * {@link #format} hook.
 *
 * <p>The point of making {@link #evaluate} {@code final} is structural, not
 * stylistic: FR-67 requires "every computed rate shows its sample size" and
 * flags anything below the threshold as low confidence. If {@code evaluate}
 * were overridable, a careless or malicious subclass could skip the gate —
 * return a confident-looking number computed from one observation and mark
 * it {@code sufficientData = true} itself. Because {@code evaluate} cannot
 * be overridden, and {@link AnalyticsResult#sampleSize()} /
 * {@link AnalyticsResult#sufficientData()} are populated only inside it
 * from {@link #sampleSize(Object)} and {@link #minimumSampleSize()} — never
 * from whatever {@link #compute} or {@link #format} return — a subclass has
 * no seam through which to fake sufficiency. It is not the subclass's
 * responsibility to enforce FR-67, so it is not the subclass's opportunity
 * to violate it either. See {@code AnalyticsMetricTest} for the test that
 * proves this with a deliberately adversarial subclass.
 *
 * @param <I> the input the metric is computed from (e.g. a list of stage
 *            durations, a set of application outcomes)
 * @param <V> the raw value type the formula produces (e.g. {@code Double}
 *            for a rate, {@code Long} for a duration in days)
 */
public abstract class AnalyticsMetric<I, V> {

    /** FR-67's default threshold: fewer than 5 observations is low confidence. Subclasses may override for a metric-specific threshold (e.g. FR-37's resume comparison also uses 5, but a future metric might not). */
    public static final int DEFAULT_MINIMUM_SAMPLE_SIZE = 5;

    /**
     * The template method. {@code final} so no subclass can rewrite the
     * skeleton itself — only the three hooks below, none of which can set
     * {@code sampleSize} or {@code sufficientData} directly.
     */
    public final AnalyticsResult<V> evaluate(I input) {
        int sampleSize = sampleSize(input);
        V rawValue = compute(input);
        boolean sufficientData = sampleSize >= minimumSampleSize();
        String formatted = format(rawValue, sufficientData);
        return new AnalyticsResult<>(rawValue, formatted, sampleSize, sufficientData);
    }

    /** How many observations the computation in {@link #compute} is based on — the number the sample-size gate actually checks. */
    protected abstract int sampleSize(I input);

    /** The formula-specific step: the one piece of this class every concrete metric exists to supply. */
    protected abstract V compute(I input);

    /** The sample-size threshold below which {@code sufficientData} is false. Override only to change the threshold, never the gating logic itself — that lives in {@link #evaluate}. */
    protected int minimumSampleSize() {
        return DEFAULT_MINIMUM_SAMPLE_SIZE;
    }

    /** Display formatting only (e.g. "%" vs "days") — has no influence over {@code sampleSize}/{@code sufficientData}, which {@link #evaluate} already decided before calling this. */
    protected String format(V value, boolean sufficientData) {
        return String.valueOf(value);
    }
}
