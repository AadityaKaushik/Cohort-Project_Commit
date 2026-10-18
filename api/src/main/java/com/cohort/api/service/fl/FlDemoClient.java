package com.cohort.api.service.fl;

import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Dev profile only: the fl-service calls behind the federated learning demo page. */
@Profile("dev")
@Component
public class FlDemoClient {
    private final RestClient flServiceRestClient;

    public FlDemoClient(RestClient flServiceRestClient) {
        this.flServiceRestClient = flServiceRestClient;
    }

    public ModelHistory modelHistory() {
        return call(() -> flServiceRestClient.get().uri("/fl/models").retrieve().body(ModelHistory.class));
    }

    public DemoStatus demoStatus() {
        return call(() -> flServiceRestClient.get().uri("/fl/demo/status").retrieve().body(DemoStatus.class));
    }

    public void setSyntheticClients(int count, int seed) {
        call(() -> flServiceRestClient.put()
                .uri("/fl/demo/synthetic-clients")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SyntheticClientsRequest(count, seed))
                .retrieve()
                .toBodilessEntity());
    }

    public void reset() {
        call(() -> flServiceRestClient.post().uri("/fl/demo/reset").retrieve().toBodilessEntity());
    }

    private <T> T call(java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException ex) {
            throw new FlServiceUnavailableException("fl-service demo call failed: " + ex.getMessage(), ex);
        }
    }

    public record ModelVersion(int version, int roundNumber, int participantCount, Instant createdAt, Double checkAccuracy, List<Double> parameters) {
    }

    public record ModelHistory(List<String> featureNames, Double centralCeiling, List<ModelVersion> versions) {
    }

    public record DemoStatus(int syntheticClients, int realClients, List<Double> noiseLevels) {
    }

    private record SyntheticClientsRequest(int count, int seed) {
    }
}
