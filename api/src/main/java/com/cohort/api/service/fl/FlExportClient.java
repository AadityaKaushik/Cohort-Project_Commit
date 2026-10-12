package com.cohort.api.service.fl;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * P51's narrow HTTP client: one method, one endpoint
 * ({@code PUT /fl/client-features/{client_id}}). Deliberately kept
 * separate from {@code FederatedLearningGateway}/{@code FlowerServiceAdapter}
 * (P52's Adapter) rather than folded into it — P51 lands as its own phase
 * before the Adapter is introduced, per {@code BUILD_PHASES_JAVA.md}'s
 * ordering. A future refactor could reasonably absorb this into the
 * gateway as a third method; it is left standalone here so each phase's
 * commit stays scoped to what that phase actually needed.
 */
@Component
public class FlExportClient {

    private final RestClient flServiceRestClient;

    public FlExportClient(RestClient flServiceRestClient) {
        this.flServiceRestClient = flServiceRestClient;
    }

    public void export(UUID userId, FlFeatureVector vector) {
        flServiceRestClient
                .put()
                .uri("/fl/client-features/{clientId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ExportRequest(1, vector.toOrderedArray()))
                .retrieve()
                .toBodilessEntity();
    }

    /** US-08: forgets this client's exported vector. Idempotent on the fl-service side. */
    public void delete(UUID userId) {
        flServiceRestClient
                .delete()
                .uri("/fl/client-features/{clientId}", userId)
                .retrieve()
                .toBodilessEntity();
    }

    /** Field names are camelCase on purpose -- the app-wide SNAKE_CASE Jackson naming strategy (application.yml) already turns this into the {@code {"schema_version": 1, "features": [...]}} wire shape fl-service's pydantic model expects, matching every other DTO in this codebase. */
    private record ExportRequest(int schemaVersion, double[] features) {
    }
}
