package com.cohort.api.dto;

import java.time.LocalDate;
import java.util.List;

/** US-45: weekly application/interview activity buckets over a date range. An empty {@code buckets} list never happens -- every week in [from, to] is present, with zero counts where there's no activity, which is what lets the frontend render a real empty-state chart instead of nothing. */
public record TimelineResponse(List<Bucket> buckets, int sampleSize) {

    public record Bucket(LocalDate weekStart, long applications, long interviews) {
    }
}
