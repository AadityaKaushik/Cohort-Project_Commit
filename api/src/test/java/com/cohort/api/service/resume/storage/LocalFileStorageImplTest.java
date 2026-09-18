package com.cohort.api.service.resume.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cohort.api.exception.FileStorageException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit test for the Strategy implementation (ARCHITECTURE_REVISION_JAVA.md §4.1) — no Spring context needed. */
class LocalFileStorageImplTest {

    @TempDir
    Path tempDir;

    @Test
    void storeThenLoadRoundTripsTheExactBytes() {
        LocalFileStorageImpl storage = new LocalFileStorageImpl(tempDir.toString());
        UUID ownerId = UUID.randomUUID();
        byte[] content = "%PDF-1.4 fake resume content".getBytes(StandardCharsets.UTF_8);
        FileUpload upload = new FileUpload("resume.pdf", "application/pdf", content);

        StoredFile stored = storage.store(ownerId, upload);
        byte[] loaded = storage.load(stored.storageKey());

        assertThat(loaded).isEqualTo(content);
        assertThat(stored.originalFilename()).isEqualTo("resume.pdf");
        assertThat(stored.contentType()).isEqualTo("application/pdf");
        assertThat(stored.sizeBytes()).isEqualTo(content.length);
    }

    @Test
    void twoStoresOfTheSameUploadGetDistinctStorageKeys() {
        LocalFileStorageImpl storage = new LocalFileStorageImpl(tempDir.toString());
        UUID ownerId = UUID.randomUUID();
        FileUpload upload = new FileUpload("resume.pdf", "application/pdf", "x".getBytes(StandardCharsets.UTF_8));

        StoredFile first = storage.store(ownerId, upload);
        StoredFile second = storage.store(ownerId, upload);

        assertThat(first.storageKey()).isNotEqualTo(second.storageKey());
    }

    @Test
    void deleteThenLoadThrows() {
        LocalFileStorageImpl storage = new LocalFileStorageImpl(tempDir.toString());
        StoredFile stored = storage.store(UUID.randomUUID(), new FileUpload("r.pdf", "application/pdf", "x".getBytes(StandardCharsets.UTF_8)));

        storage.delete(stored.storageKey());

        assertThatThrownBy(() -> storage.load(stored.storageKey())).isInstanceOf(FileStorageException.class);
    }

    @Test
    void deletingAMissingKeyIsNotAnError() {
        LocalFileStorageImpl storage = new LocalFileStorageImpl(tempDir.toString());

        storage.delete(UUID.randomUUID() + "/" + UUID.randomUUID());
    }

    @Test
    void storageKeyCannotEscapeTheStorageRoot() {
        LocalFileStorageImpl storage = new LocalFileStorageImpl(tempDir.toString());

        assertThatThrownBy(() -> storage.load("../../etc/passwd")).isInstanceOf(IllegalArgumentException.class);
    }
}
