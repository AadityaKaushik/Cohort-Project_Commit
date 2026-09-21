package com.cohort.api.service.fl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

/**
 * P52 (Adapter pattern): {@link FlowerServiceAdapter} against a stubbed
 * fl-service — a hand-rolled JDK {@code HttpServer} (see
 * {@link StubFlServiceServer}), the simplest reliable stubbing option for
 * this codebase (no existing WireMock/MockWebServer precedent). Proves the
 * adapter correctly parses a real {@code GET /fl/model} response shape and
 * turns any transport failure into {@link FlServiceUnavailableException} —
 * exactly the exception type P53's circuit breaker is configured around.
 */
class FlowerServiceAdapterContractTest {

    private StubFlServiceServer stub;
    private FlowerServiceAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        stub = new StubFlServiceServer();
        adapter = new FlowerServiceAdapter(restClientFor(stub.baseUrl()));
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    @Test
    void parsesARealFlModelResponseIntoFlModelInfo() {
        stub.respondWith("""
                {"version": 7, "round_number": 7, "participant_count": 4, "created_at": "2026-03-01T12:30:00Z", "parameters": [0.5, -0.2, 0.1]}
                """);

        FlModelInfo model = adapter.fetchCurrentModel();

        assertThat(model.version()).isEqualTo(7);
        assertThat(model.roundNumber()).isEqualTo(7);
        assertThat(model.participantCount()).isEqualTo(4);
        assertThat(model.createdAt()).isEqualTo(Instant.parse("2026-03-01T12:30:00Z"));
        assertThat(model.parameters()).containsExactly(0.5, -0.2, 0.1);
    }

    @Test
    void wrapsAMalformedResponseAsFlServiceUnavailable() {
        stub.respondWith("not valid json");

        assertThatThrownBy(() -> adapter.fetchCurrentModel()).isInstanceOf(FlServiceUnavailableException.class);
    }

    @Test
    void wrapsAConnectionFailureAsFlServiceUnavailable() {
        stub.stop(); // simulate fl-service being down before the call is even made

        assertThatThrownBy(() -> adapter.fetchCurrentModel()).isInstanceOf(FlServiceUnavailableException.class);
    }

    /** Mirrors what FlServiceConfig wires in production -- SNAKE_CASE Jackson naming (application.yml's app-wide convention) plus JavaTimeModule for the ISO timestamp, without needing a full Spring context. */
    private RestClient restClientFor(String baseUrl) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(new MappingJackson2HttpMessageConverter(mapper));
                })
                .build();
    }
}
