package com.cohort.api.service.fl;

/** One past interview round: the features as they were just before it, and whether the user rated it 4 or 5. */
public record FlTrainingExample(FlFeatureVector features, boolean confident) {
}
