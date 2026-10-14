package com.cohort.api.service.resume.storage;

import com.cohort.api.exception.FileTooLargeException;
import com.cohort.api.service.resume.storage.validation.ExtensionValidationHandler;
import com.cohort.api.service.resume.storage.validation.MagicByteValidationHandler;
import com.cohort.api.service.resume.storage.validation.MimeTypeValidationHandler;
import com.cohort.api.service.resume.storage.validation.UploadValidationHandler;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ValidatingFileStorage implements FileStorageStrategy {
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final FileStorageStrategy delegate;
    private final UploadValidationHandler validationChain;

    public ValidatingFileStorage(@Qualifier("localFileStorageImpl") FileStorageStrategy delegate) {
        this.delegate = delegate;
        this.validationChain = buildValidationChain();
    }

    private static UploadValidationHandler buildValidationChain() {
        UploadValidationHandler extensionCheck = new ExtensionValidationHandler();
        UploadValidationHandler mimeTypeCheck = new MimeTypeValidationHandler();
        UploadValidationHandler magicByteCheck = new MagicByteValidationHandler();
        extensionCheck.linkTo(mimeTypeCheck).linkTo(magicByteCheck);
        return extensionCheck;
    }

    @Override
    public StoredFile store(UUID ownerId, FileUpload upload) {
        if (upload.sizeBytes() > MAX_FILE_SIZE_BYTES) {
            throw new FileTooLargeException("Resume file exceeds the 5 MB limit (" + upload.sizeBytes() + " bytes).");
        }
        validationChain.handle(upload);
        return delegate.store(ownerId, upload);
    }

    @Override
    public byte[] load(String storageKey) {
        return delegate.load(storageKey);
    }

    @Override
    public void delete(String storageKey) {
        delegate.delete(storageKey);
    }
}
