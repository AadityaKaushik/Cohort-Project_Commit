package com.cohort.api.dto;

import java.util.UUID;

/** Topic is optional when the interview question already has one. */
public record SaveToQuestionBankRequest(UUID topicId) {
}
