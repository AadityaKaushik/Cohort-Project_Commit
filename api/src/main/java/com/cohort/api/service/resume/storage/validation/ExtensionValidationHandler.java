package com.cohort.api.service.resume.storage.validation;

import com.cohort.api.exception.InvalidFileException;
import com.cohort.api.service.resume.storage.FileUpload;
import java.util.Set;

/** Stage 1 of the validation chain: the cheapest, weakest check — a client can lie about this trivially, which is exactly why two more stages follow it. */
public class ExtensionValidationHandler extends UploadValidationHandler {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    @Override
    protected void doValidate(FileUpload upload) {
        String filename = upload.originalFilename();
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        String extension = dot < 0 || dot == filename.length() - 1 ? "" : filename.substring(dot + 1).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException("Unsupported file extension: ." + extension + " (allowed: pdf, doc, docx)");
        }
    }
}
