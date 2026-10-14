package com.cohort.api.service.ai;

public record AiMessage(String role, String content) {
    public static AiMessage user(String content) {
        return new AiMessage("user", content);
    }

    public static AiMessage assistant(String content) {
        return new AiMessage("assistant", content);
    }
}
