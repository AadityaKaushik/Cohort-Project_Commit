package com.cohort.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrepPackResponse(
        UUID interviewId,
        String companyName,
        String jobTitle,
        int roundNumber,
        String roundType,
        Instant scheduledAt,
        String interviewerName,
        String interviewerRole,
        List<String> companyNotes,
        List<PastRound> pastRounds,
        List<AskedQuestion> questionsAsked,
        List<WeakTopic> weakTopics,
        List<String> resumeHighlights,
        Double readinessScore,
        String readinessFormatted) {

    public record PastRound(
            int roundNumber,
            String roundType,
            String jobTitle,
            String outcome,
            String wentWell,
            String wentPoorly,
            String improvementAreas) {
    }

    public record AskedQuestion(String text, String category, int roundNumber) {
    }

    public record WeakTopic(String name, boolean hasEvidence, int masteryPercent) {
    }
}
