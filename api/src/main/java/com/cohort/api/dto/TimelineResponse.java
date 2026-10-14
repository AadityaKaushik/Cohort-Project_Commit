package com.cohort.api.dto;

import java.time.LocalDate;
import java.util.List;

public record TimelineResponse(List<Bucket> buckets, int sampleSize) {
    public record Bucket(LocalDate weekStart, long applications, long interviews) {
    }
}
