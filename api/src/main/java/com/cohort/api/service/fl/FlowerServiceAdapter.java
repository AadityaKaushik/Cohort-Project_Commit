package com.cohort.api.service.fl;

import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adapter pattern (ARCHITECTURE_REVISION_JAVA.md §4.1): the only class in
 * this codebase that knows fl-service's actual REST contract
 * ({@code GET /fl/model}, P49) — translates its JSON shape into
 * {@link FlModelInfo} and its failure modes into one exception type,
 * {@link FlServiceUnavailableException}, that P53's circuit breaker knows
 * how to react to.
 */
@Component
public class FlowerServiceAdapter implements FederatedLearningGateway {

    private final RestClient flServiceRestClient;

    public FlowerServiceAdapter(RestClient flServiceRestClient) {
        this.flServiceRestClient = flServiceRestClient;
    }

    @Override
    public FlModelInfo fetchCurrentModel() {
        try {
            ModelResponse response = flServiceRestClient
                    .get()
                    .uri("/fl/model")
                    .retrieve()
                    .body(ModelResponse.class);
            if (response == null) {
                throw new FlServiceUnavailableException("fl-service returned an empty /fl/model response", null);
            }
            return new FlModelInfo(
                    response.version(), response.roundNumber(), response.participantCount(),
                    response.createdAt(), response.parameters());
        } catch (RestClientException ex) {
            throw new FlServiceUnavailableException("fl-service /fl/model call failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public FlRoundOutcome runRound(int roundNumber, List<String> clientIds) {
        try {
            RoundResponse response = flServiceRestClient
                    .post()
                    .uri("/fl/round")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RoundRequest(roundNumber, List.copyOf(clientIds)))
                    .retrieve()
                    .body(RoundResponse.class);
            if (response == null) {
                throw new FlServiceUnavailableException("fl-service returned an empty /fl/round response", null);
            }
            List<FlLedgerEntryData> entries = response.entries().stream()
                    .map(e -> new FlLedgerEntryData(
                            e.clientId(), e.schemaVersion(), e.preClipNorm(), e.postClipNorm(),
                            e.noiseMultiplier(), e.epsilon(), e.transmittedVector()))
                    .toList();
            return new FlRoundOutcome(response.roundNumber(), response.accepted(), response.reason(), response.cohortSize(), entries);
        } catch (RestClientException ex) {
            throw new FlServiceUnavailableException("fl-service /fl/round call failed: " + ex.getMessage(), ex);
        }
    }

    /** Mirrors fl-service's {@code app/control/api.py::_serialise} response shape exactly. */
    private record ModelResponse(int version, int roundNumber, int participantCount, Instant createdAt, List<Double> parameters) {
    }

    private record RoundRequest(int roundNumber, List<String> clientIds) {
    }

    /** Mirrors fl-service's {@code POST /fl/round} response shape exactly (app/control/api.py). */
    private record RoundResponse(int roundNumber, boolean accepted, String reason, int cohortSize, List<RoundEntry> entries) {
    }

    private record RoundEntry(
            String clientId, int schemaVersion, double preClipNorm, double postClipNorm,
            double noiseMultiplier, double epsilon, List<Double> transmittedVector) {
    }
}
