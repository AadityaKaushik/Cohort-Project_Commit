package com.cohort.api.service.fl;

import java.util.List;

/** Java's view of fl-service's {@code POST /fl/round} response (US-61/US-63, P55). */
public record FlRoundOutcome(int roundNumber, boolean accepted, String reason, int cohortSize, List<FlLedgerEntryData> entries) {
}
