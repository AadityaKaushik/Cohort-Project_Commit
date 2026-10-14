package com.cohort.api.service.fl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

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
        stub.stop();

        assertThatThrownBy(() -> adapter.fetchCurrentModel()).isInstanceOf(FlServiceUnavailableException.class);
    }

    @Test
    void parsesARealFlRoundResponseIntoFlRoundOutcome() {
        stub.respondToRoundWith("""
                {"round_number": 4, "accepted": true, "reason": null, "cohort_size": 3, "entries": [
                    {"client_id": "11111111-1111-1111-1111-111111111111", "schema_version": 1,
                     "pre_clip_norm": 1.4, "post_clip_norm": 1.0, "noise_multiplier": 1.0, "epsilon": 1.0,
                     "transmitted_vector": [0.1, 0.2, 0.3]}
                ]}
                """);

        FlRoundOutcome outcome = adapter.runRound(4, List.of("11111111-1111-1111-1111-111111111111"));

        assertThat(outcome.roundNumber()).isEqualTo(4);
        assertThat(outcome.accepted()).isTrue();
        assertThat(outcome.reason()).isNull();
        assertThat(outcome.cohortSize()).isEqualTo(3);
        assertThat(outcome.entries()).hasSize(1);
        FlLedgerEntryData entry = outcome.entries().get(0);
        assertThat(entry.clientId()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(entry.preClipNorm()).isEqualTo(1.4);
        assertThat(entry.postClipNorm()).isEqualTo(1.0);
        assertThat(entry.transmittedVector()).containsExactly(0.1, 0.2, 0.3);
    }

    @Test
    void sendsTheRoundNumberAndTheOptedInClientIdsInFlServicesSnakeCaseShape() throws Exception {
        adapter.runRound(7, List.of("user-a", "user-b"));

        var sent = new ObjectMapper().readTree(stub.lastRoundRequestJson());
        assertThat(sent.get("round_number").asInt()).isEqualTo(7);
        assertThat(sent.get("client_ids").get(0).asText()).isEqualTo("user-a");
        assertThat(sent.get("client_ids").get(1).asText()).isEqualTo("user-b");
    }

    @Test
    void parsesARefusedRoundWithAReason() {
        stub.respondToRoundWith("""
                {"round_number": 2, "accepted": false, "reason": "round 2 refused: 1 participant(s) is below the minimum cohort size of 3", "cohort_size": 1, "entries": []}
                """);

        FlRoundOutcome outcome = adapter.runRound(2, List.of("a"));

        assertThat(outcome.accepted()).isFalse();
        assertThat(outcome.reason()).contains("minimum cohort size");
        assertThat(outcome.entries()).isEmpty();
    }

    @Test
    void wrapsARoundConnectionFailureAsFlServiceUnavailable() {
        stub.stop();

        assertThatThrownBy(() -> adapter.runRound(1, List.of())).isInstanceOf(FlServiceUnavailableException.class);
    }

    // Mirrors what FlServiceConfig wires in production -- SNAKE_CASE Jackson naming (application.yml's app-wide convention) plus JavaTimeModule for the ISO timestamp, without needing a full Spring context.
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
