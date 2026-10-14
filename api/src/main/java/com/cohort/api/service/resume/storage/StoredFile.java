package com.cohort.api.service.resume.storage;

public record StoredFile(String storageKey, String originalFilename, String contentType, long sizeBytes) {
}
