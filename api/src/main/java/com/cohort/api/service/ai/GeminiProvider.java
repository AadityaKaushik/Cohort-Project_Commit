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

public class GeminiProvider implements AiProvider {
    private static final String DATA_PREFIX = "data:";

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final String model;
    private final String apiKey;
    private final Duration readTimeout;

    public GeminiProvider(
            HttpClient client, ObjectMapper mapper, String baseUrl, String model, String apiKey, Duration readTimeout) {
        this.client = client;
        this.mapper = mapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.model = model;
        this.apiKey = apiKey;
        this.readTimeout = readTimeout;
    }

    @Override
    public String id() {
        return "gemini";
    }

    @Override
    public boolean hosted() {
        return true;
    }

    @Override
    public String complete(AiPrompt prompt) {
        HttpResponse<String> response = send(prompt, ":generateContent", HttpResponse.BodyHandlers.ofString());
        try {
            return textOf(mapper.readTree(response.body()));
        } catch (IOException ex) {
            throw new AiUnavailableException(ex);
        }
    }

    @Override
    public void stream(AiPrompt prompt, Consumer<String> onToken) {
        HttpResponse<Stream<String>> response =
                send(prompt, ":streamGenerateContent?alt=sse", HttpResponse.BodyHandlers.ofLines());
        try (Stream<String> lines = response.body()) {
            lines.filter(line -> line.startsWith(DATA_PREFIX)).forEach(line -> {
                try {
                    String token = textOf(mapper.readTree(line.substring(DATA_PREFIX.length()).trim()));
                    if (!token.isEmpty()) {
                        onToken.accept(token);
                    }
                } catch (IOException ex) {
                    throw new AiUnavailableException(ex);
                }
            });
        }
    }

    private String textOf(JsonNode node) {
        StringBuilder text = new StringBuilder();
        for (JsonNode part : node.path("candidates").path(0).path("content").path("parts")) {
            text.append(part.path("text").asText(""));
        }
        return text.toString();
    }

    private <T> HttpResponse<T> send(AiPrompt prompt, String action, HttpResponse.BodyHandler<T> handler) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", prompt.system()))));
        body.put("contents", contents(prompt));
        if (prompt.jsonOutput()) {
            body.put("generationConfig", Map.of("responseMimeType", "application/json"));
        }
        String separator = action.contains("?") ? "&" : "?";
        URI uri = URI.create(baseUrl + "/v1beta/models/" + model + action + separator + "key=" + apiKey);
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
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

    private List<Map<String, Object>> contents(AiPrompt prompt) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (AiMessage message : prompt.messages()) {
            String role = "assistant".equals(message.role()) ? "model" : "user";
            contents.add(Map.of("role", role, "parts", List.of(Map.of("text", message.content()))));
        }
        return contents;
    }
}
