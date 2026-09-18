package com.cohort.api.service.resume.storage.validation;

import com.cohort.api.service.resume.storage.FileUpload;

/**
 * Chain of Responsibility pattern (ARCHITECTURE_REVISION_JAVA.md §4.1):
 * base handler for one stage of resume-upload validation. Each concrete
 * handler validates exactly one thing and, if it passes, forwards to the
 * next handler in the chain — so extension, MIME, and magic-byte checks
 * ({@link ExtensionValidationHandler}, {@link MimeTypeValidationHandler},
 * {@link MagicByteValidationHandler}) are independently testable and can be
 * reordered, replaced, or extended (e.g. a future virus-scan stage) without
 * any existing handler changing.
 *
 * <p>Unlike the classic textbook shape where a handler may choose not to
 * pass a request further, every stage here either approves (and forwards)
 * or rejects by throwing — a resume upload has no notion of "this handler
 * doesn't apply, try the next one," every stage must pass.
 */
public abstract class UploadValidationHandler {

    private UploadValidationHandler next;

    /** Links {@code next} after this handler and returns it, so calls chain fluently: {@code a.linkTo(b).linkTo(c)}. */
    public UploadValidationHandler linkTo(UploadValidationHandler next) {
        this.next = next;
        return next;
    }

    /** Runs this handler's check, then forwards to the next handler (if any). Throws on the first failing stage. */
    public final void handle(FileUpload upload) {
        doValidate(upload);
        if (next != null) {
            next.handle(upload);
        }
    }

    protected abstract void doValidate(FileUpload upload);
}
