package com.cohort.api.service.resume.storage;

public record FileUpload(String originalFilename, String contentType, byte[] content) {
    public long sizeBytes() {
        return content.length;
    }
}
