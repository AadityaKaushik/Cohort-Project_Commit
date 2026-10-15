package com.cohort.api.dto;

import java.time.LocalDate;
import java.util.List;

public record ReviewQueueResponse(LocalDate asOf, List<ReviewItemResponse> items) {
}
