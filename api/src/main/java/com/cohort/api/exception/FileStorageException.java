package com.cohort.api.exception;

/** Wraps any I/O failure from a {@link com.cohort.api.service.resume.storage.FileStorageStrategy} implementation as unchecked. */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
