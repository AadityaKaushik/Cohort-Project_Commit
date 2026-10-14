package com.cohort.api.service.ai;

import com.cohort.api.exception.AiUnavailableException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StubAiProvider implements AiProvider {
    private final List<AiPrompt> prompts = new ArrayList<>();
    private String reply = "";
    private boolean down;
    private boolean hosted;

    @Override
    public String id() {
        return hosted ? "gemini" : "ollama";
    }

    @Override
    public boolean hosted() {
        return hosted;
    }

    @Override
    public String complete(AiPrompt prompt) {
        prompts.add(prompt);
        if (down) {
            throw new AiUnavailableException();
        }
        return reply;
    }

    @Override
    public void stream(AiPrompt prompt, Consumer<String> onToken) {
        prompts.add(prompt);
        if (down) {
            throw new AiUnavailableException();
        }
        for (String word : reply.split("(?<= )")) {
            onToken.accept(word);
        }
    }

    public void replyWith(String text) {
        this.reply = text;
        this.down = false;
    }

    public void goDown() {
        this.down = true;
    }

    public void setHosted(boolean hosted) {
        this.hosted = hosted;
    }

    public List<AiPrompt> prompts() {
        return prompts;
    }

    public AiPrompt lastPrompt() {
        return prompts.get(prompts.size() - 1);
    }

    public void reset() {
        prompts.clear();
        reply = "";
        down = false;
        hosted = false;
    }

    public String everythingSent() {
        StringBuilder all = new StringBuilder();
        prompts.forEach(prompt -> {
            all.append(prompt.system()).append('\n');
            prompt.messages().forEach(message -> all.append(message.content()).append('\n'));
        });
        return all.toString();
    }
}
