package com.cohort.api.service.ai;

import java.util.List;

public record AiPrompt(String system, List<AiMessage> messages, boolean jsonOutput) {
    public static AiPrompt text(String system, List<AiMessage> messages) {
        return new AiPrompt(system, messages, false);
    }

    public static AiPrompt json(String system, String userText) {
        return new AiPrompt(system, List.of(AiMessage.user(userText)), true);
    }

    public int characterCount() {
        return system.length() + messages.stream().mapToInt(message -> message.content().length()).sum();
    }
}
