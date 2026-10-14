package com.cohort.api.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cohort.api.exception.AiUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiProviderContractTest {
    private static final AiPrompt PROMPT = AiPrompt.text("be brief", List.of(AiMessage.user("hello")));

    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private HttpServer server;
    private int status = 200;
    private String responseBody = "";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().toString());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private OllamaProvider ollama() {
        return new OllamaProvider(HttpClient.newHttpClient(), mapper, baseUrl(), "test-model", Duration.ofSeconds(5));
    }

    private GeminiProvider gemini() {
        return new GeminiProvider(HttpClient.newHttpClient(), mapper, baseUrl(), "test-model", "k3y", Duration.ofSeconds(5));
    }

    @Test
    void ollamaCompleteReadsMessageContent() throws Exception {
        responseBody = "{\"message\":{\"role\":\"assistant\",\"content\":\"hi there\"},\"done\":true}";

        assertThat(ollama().complete(PROMPT)).isEqualTo("hi there");

        JsonNode sent = mapper.readTree(requestBody.get());
        assertThat(requestPath.get()).isEqualTo("/api/chat");
        assertThat(sent.get("stream").asBoolean()).isFalse();
        assertThat(sent.get("messages").get(0).get("role").asText()).isEqualTo("system");
        assertThat(sent.get("messages").get(1).get("content").asText()).isEqualTo("hello");
    }

    @Test
    void ollamaStreamEmitsEachChunk() {
        responseBody = """
                {"message":{"content":"Hel"},"done":false}
                {"message":{"content":"lo"},"done":false}
                {"message":{"content":""},"done":true}
                """;
        List<String> tokens = new ArrayList<>();

        ollama().stream(PROMPT, tokens::add);

        assertThat(tokens).containsExactly("Hel", "lo");
    }

    @Test
    void ollamaJsonPromptAsksForJsonFormat() throws Exception {
        responseBody = "{\"message\":{\"content\":\"{}\"}}";

        ollama().complete(AiPrompt.json("sys", "text"));

        assertThat(mapper.readTree(requestBody.get()).get("format").asText()).isEqualTo("json");
    }

    @Test
    void geminiCompleteJoinsParts() throws Exception {
        responseBody = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"a\"},{\"text\":\"b\"}]}}]}";

        assertThat(gemini().complete(PROMPT)).isEqualTo("ab");

        assertThat(requestPath.get()).isEqualTo("/v1beta/models/test-model:generateContent?key=k3y");
        JsonNode sent = mapper.readTree(requestBody.get());
        assertThat(sent.get("systemInstruction").get("parts").get(0).get("text").asText()).isEqualTo("be brief");
        assertThat(sent.get("contents").get(0).get("role").asText()).isEqualTo("user");
    }

    @Test
    void geminiStreamReadsServerSentEvents() {
        responseBody = """
                data: {"candidates":[{"content":{"parts":[{"text":"one "}]}}]}

                data: {"candidates":[{"content":{"parts":[{"text":"two"}]}}]}

                """;
        List<String> tokens = new ArrayList<>();

        gemini().stream(PROMPT, tokens::add);

        assertThat(tokens).containsExactly("one ", "two");
        assertThat(requestPath.get()).contains(":streamGenerateContent?alt=sse&key=k3y");
    }

    @Test
    void errorStatusBecomesUnavailable() {
        status = 500;

        assertThatThrownBy(() -> ollama().complete(PROMPT)).isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(() -> gemini().stream(PROMPT, token -> { })).isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void unreachableServerBecomesUnavailable() {
        server.stop(0);

        assertThatThrownBy(() -> ollama().complete(PROMPT)).isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void identifiesItselfAndWhetherItIsHosted() {
        assertThat(ollama().id()).isEqualTo("ollama");
        assertThat(ollama().hosted()).isFalse();
        assertThat(gemini().id()).isEqualTo("gemini");
        assertThat(gemini().hosted()).isTrue();
    }
}
