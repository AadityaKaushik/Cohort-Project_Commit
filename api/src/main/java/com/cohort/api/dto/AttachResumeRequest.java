package com.cohort.api.dto;

import java.util.UUID;

/**
 * US-24/FR-34: associates (or re-associates) exactly one resume with an
 * application. {@code resumeId} is deliberately nullable — a null value
 * clears the association — so this one endpoint handles set, change, and
 * detach.
 */
public record AttachResumeRequest(UUID resumeId) {
}
