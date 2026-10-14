package com.cohort.api.service.resume.storage.validation;

import com.cohort.api.service.resume.storage.FileUpload;

public abstract class UploadValidationHandler {
    private UploadValidationHandler next;

    public UploadValidationHandler linkTo(UploadValidationHandler next) {
        this.next = next;
        return next;
    }

    public final void handle(FileUpload upload) {
        doValidate(upload);
        if (next != null) {
            next.handle(upload);
        }
    }

    protected abstract void doValidate(FileUpload upload);
}
