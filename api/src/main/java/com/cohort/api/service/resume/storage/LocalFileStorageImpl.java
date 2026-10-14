package com.cohort.api.service.resume.storage;

import com.cohort.api.exception.FileStorageException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalFileStorageImpl implements FileStorageStrategy {
    private final Path baseDirectory;

    public LocalFileStorageImpl(@Value("${app.resume-storage.base-dir:./data/resumes}") String baseDir) {
        this.baseDirectory = Paths.get(baseDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create resume storage directory: " + baseDirectory, e);
        }
    }

    @Override
    public StoredFile store(UUID ownerId, FileUpload upload) {
        String storageKey = ownerId + "/" + UUID.randomUUID();
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, upload.content(), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file for owner " + ownerId, e);
        }
        return new StoredFile(storageKey, upload.originalFilename(), upload.contentType(), upload.sizeBytes());
    }

    @Override
    public byte[] load(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        } catch (IOException e) {
            throw new FileStorageException("Failed to load file " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file " + storageKey, e);
        }
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key must not be blank");
        }
        Path resolved = baseDirectory.resolve(storageKey).normalize();
        if (!resolved.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("Storage key escapes the storage root: " + storageKey);
        }
        return resolved;
    }
}
