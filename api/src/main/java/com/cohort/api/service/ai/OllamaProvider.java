package com.cohort.api.service.ai;

import com.cohort.api.exception.AiUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class OllamaProvider implements AiProvider {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final String model;
    private final Duration readTimeout;

    public OllamaProvider(HttpClient client, ObjectMapper mapper, String baseUrl, String model, Duration readTimeout) {
        this.client = client;
        this.mapper = mapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.model = model;
        this.readTimeout = readTimeout;
    }

    @Override
    public String id() {
        return "ollama";
    }

    @Override
    public boolean hosted() {
        return false;
    }

    @Override
    public String complete(AiPrompt prompt) {
        HttpResponse<String> response = send(prompt, false, HttpResponse.BodyHandlers.ofString());
        try {
            return mapper.readTree(response.body()).path("message").path("content").asText("");
        } catch (IOException ex) {
            throw new AiUnavailableException(ex);
        }
    }

    @Override
    public void stream(AiPrompt prompt, Consumer<String> onToken) {
        HttpResponse<Stream<String>> response = send(prompt, true, HttpResponse.BodyHandlers.ofLines());
        try (Stream<String> lines = response.body()) {
            lines.filter(line -> !line.isBlank()).forEach(line -> {
                try {
                    JsonNode chunk = mapper.readTree(line);
                    String token = chunk.path("message").path("content").asText("");
                    if (!token.isEmpty()) {
                        onToken.accept(token);
                    }
                } catch (IOException ex) {
                    throw new AiUnavailableException(ex);
                }
            });
        }
    }

    private <T> HttpResponse<T> send(AiPrompt prompt, boolean stream, HttpResponse.BodyHandler<T> handler) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("stream", stream);
        body.put("messages", chatMessages(prompt));
        if (prompt.jsonOutput()) {
            body.put("format", "json");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
                    .timeout(readTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<T> response = client.send(request, handler);
            if (response.statusCode() / 100 != 2) {
                throw new AiUnavailableException();
            }
            return response;
        } catch (IOException ex) {
            throw new AiUnavailableException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException(ex);
        }
    }

    private List<Map<String, String>> chatMessages(AiPrompt prompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", prompt.system()));
        prompt.messages().forEach(message -> messages.add(Map.of("role", message.role(), "content", message.content())));
        return messages;
    }
}
