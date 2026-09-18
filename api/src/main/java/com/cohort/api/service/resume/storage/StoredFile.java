package com.cohort.api.service.resume.storage;

/** What a {@link FileStorageStrategy} hands back after a successful store — everything {@code Resume}'s file_* columns need. */
public record StoredFile(String storageKey, String originalFilename, String contentType, long sizeBytes) {
}
