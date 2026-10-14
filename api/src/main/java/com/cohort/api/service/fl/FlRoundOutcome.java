package com.cohort.api.service.fl;

import java.util.List;

public record FlRoundOutcome(int roundNumber, boolean accepted, String reason, int cohortSize, List<FlLedgerEntryData> entries) {
}
