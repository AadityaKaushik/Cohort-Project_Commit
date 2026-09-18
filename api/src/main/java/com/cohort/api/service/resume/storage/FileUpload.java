package com.cohort.api.service.resume.storage;

/**
 * An in-memory file ready to be stored. Resumes are capped at 5 MB (FR-30),
 * so holding the whole file in memory rather than streaming it keeps this
 * abstraction — and the validation chain in {@code ValidatingFileStorage}
 * (P19), which needs to inspect the header bytes — simple, at negligible
 * memory cost.
 */
public record FileUpload(String originalFilename, String contentType, byte[] content) {

    public long sizeBytes() {
        return content.length;
    }
}
