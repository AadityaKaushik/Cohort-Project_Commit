package com.cohort.api.dto;

import java.util.List;

public record ResumeMatchResponse(
        boolean hasJobDescription,
        boolean hasResume,
        List<String> required,
        List<String> matched,
        List<String> missing,
        Integer score,
        String explanation,
        List<TailoringBullet> bullets) {

    public record TailoringBullet(String text, List<String> keywords, String source) {
    }
}
