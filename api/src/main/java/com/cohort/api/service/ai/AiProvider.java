package com.cohort.api.service.ai;

import com.cohort.api.exception.AiUnavailableException;
import java.util.function.Consumer;

public interface AiProvider {
    String id();

    boolean hosted();

    String complete(AiPrompt prompt) throws AiUnavailableException;

    void stream(AiPrompt prompt, Consumer<String> onToken) throws AiUnavailableException;
}
