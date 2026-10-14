package com.cohort.api.service.fl;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public void delete(UUID userId) {
        flServiceRestClient
                .delete()
                .uri("/fl/client-features/{clientId}", userId)
                .retrieve()
                .toBodilessEntity();
    }

    private record ExportRequest(int schemaVersion, double[] features) {
    }
}
