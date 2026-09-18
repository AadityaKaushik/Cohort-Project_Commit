package com.cohort.api.service.resume.storage;

import java.util.UUID;

/**
 * Strategy pattern (ARCHITECTURE_REVISION_JAVA.md §4.1): the algorithm for
 * <em>where and how a resume file's bytes are persisted</em> is swappable
 * behind this interface — local disk today ({@link LocalFileStorageImpl}),
 * S3 or another object store later — without the calling code
 * ({@code ResumeService}, and the {@code ValidatingFileStorage} decorator
 * from P19) ever changing.
 */
public interface FileStorageStrategy {

    /** Persists {@code upload} for {@code ownerId} and returns the key needed to load or delete it again. */
    StoredFile store(UUID ownerId, FileUpload upload);

    /** Loads the raw bytes previously stored under {@code storageKey}. */
    byte[] load(String storageKey);

    /** Deletes the file at {@code storageKey}; a missing file is not an error (idempotent). */
    void delete(String storageKey);
}
