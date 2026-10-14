package com.cohort.api.service.resume.storage;

import java.util.UUID;

public interface FileStorageStrategy {
    StoredFile store(UUID ownerId, FileUpload upload);

    byte[] load(String storageKey);

    void delete(String storageKey);
}
