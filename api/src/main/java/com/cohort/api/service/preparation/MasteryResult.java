package com.cohort.api.service.preparation;

public record MasteryResult(double rawScore, double masteryScore, int observationCount, boolean sufficientEvidence) {
}
