package com.cohort.api.service.fl;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public final class StubFlServiceServer implements AutoCloseable {
    private final HttpServer server;
    private final AtomicReference<String> modelResponseJson = new AtomicReference<>(defaultModelJson());
    private final AtomicReference<String> roundResponseJson = new AtomicReference<>(defaultRoundJson());
    private final AtomicReference<String> lastRoundRequestJson = new AtomicReference<>();

    public StubFlServiceServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/fl/model", exchange -> {
            byte[] body = modelResponseJson.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/fl/round", exchange -> {
            lastRoundRequestJson.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = roundResponseJson.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.setExecutor(null);
        server.start();
    }

    public String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    public void respondWith(String json) {
        modelResponseJson.set(json);
    }

    public void respondToRoundWith(String json) {
        roundResponseJson.set(json);
    }

    public String lastRoundRequestJson() {
        return lastRoundRequestJson.get();
    }

    public void stop() {
        server.stop(0);
    }

    @Override
    public void close() {
        stop();
    }

    private static String defaultModelJson() {
        return """
                {"version": 3, "round_number": 3, "participant_count": 5, "created_at": "2026-01-01T00:00:00Z", "parameters": [0.1, 0.2, 0.3]}
                """;
    }

    private static String defaultRoundJson() {
        return """
                {"round_number": 1, "accepted": true, "reason": null, "cohort_size": 3, "entries": []}
                """;
    }
}
