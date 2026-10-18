package com.cohort.api.service.fl;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
        return runRound(roundNumber, clientIds, null);
    }

    @Override
    public FlRoundOutcome runRound(int roundNumber, List<String> clientIds, Double noiseMultiplier) {
        try {
            RoundResponse response = flServiceRestClient
                    .post()
                    .uri("/fl/round")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RoundRequest(roundNumber, List.copyOf(clientIds), noiseMultiplier))
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
            return new FlRoundOutcome(
                    response.roundNumber(), response.accepted(), response.reason(), response.cohortSize(), entries,
                    response.satOutCount() == null ? 0 : response.satOutCount(), response.modelVersion(), response.checkAccuracy());
        } catch (RestClientException ex) {
            throw new FlServiceUnavailableException("fl-service /fl/round call failed: " + ex.getMessage(), ex);
        }
    }

    private record ModelResponse(int version, int roundNumber, int participantCount, Instant createdAt, List<Double> parameters) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record RoundRequest(int roundNumber, List<String> clientIds, Double noiseMultiplier) {
    }

    private record RoundResponse(
            int roundNumber, boolean accepted, String reason, int cohortSize, List<RoundEntry> entries,
            Integer satOutCount, Integer modelVersion, Double checkAccuracy) {
    }

    private record RoundEntry(
            String clientId, int schemaVersion, double preClipNorm, double postClipNorm,
            double noiseMultiplier, double epsilon, List<Double> transmittedVector) {
    }
}
