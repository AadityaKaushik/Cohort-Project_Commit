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
    private final AtomicReference<String> modelsResponseJson = new AtomicReference<>(defaultModelsJson());
    private final java.util.List<String> demoCalls = new java.util.concurrent.CopyOnWriteArrayList<>();

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
        server.createContext("/fl/models", exchange -> {
            byte[] body = modelsResponseJson.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/fl/demo", exchange -> {
            demoCalls.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            exchange.getRequestBody().readAllBytes();
            if (exchange.getRequestURI().getPath().endsWith("/status")) {
                byte[] body = "{\"synthetic_clients\": 20, \"real_clients\": 1, \"noise_levels\": [1.0, 0.5, 0.25]}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } else {
                exchange.sendResponseHeaders(204, -1);
            }
            exchange.close();
        });
        server.createContext("/fl/client-features", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(204, -1);
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

    public java.util.List<String> demoCalls() {
        return demoCalls;
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

    private static String defaultModelsJson() {
        StringBuilder names = new StringBuilder();
        StringBuilder parameters = new StringBuilder();
        for (int i = 0; i < FlFeatureSchema.FEATURE_NAMES.size(); i++) {
            names.append(i == 0 ? "" : ",").append('"').append(FlFeatureSchema.FEATURE_NAMES.get(i)).append('"');
            parameters.append(i == 0 ? "" : ",").append(i == 0 ? "1.5" : "0.0");
        }
        parameters.append(",-1.0");
        return ("{\"feature_names\": [%s], \"central_ceiling\": 0.74, \"versions\": ["
                + "{\"version\": 0, \"round_number\": 0, \"participant_count\": 0, \"created_at\": \"2026-01-01T00:00:00Z\", \"check_accuracy\": 0.52, \"parameters\": [%s]},"
                + "{\"version\": 1, \"round_number\": 1, \"participant_count\": 21, \"created_at\": \"2026-01-01T00:05:00Z\", \"check_accuracy\": 0.58, \"parameters\": [%s]}]}")
                .formatted(names, parameters.toString().replace("1.5", "0.0").replace("-1.0", "0.0"), parameters);
    }

    private static String defaultRoundJson() {
        return """
                {"round_number": 1, "accepted": true, "reason": null, "cohort_size": 3, "entries": []}
                """;
    }
}
