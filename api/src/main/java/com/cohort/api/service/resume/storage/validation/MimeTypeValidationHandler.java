package com.cohort.api.service.resume.storage.validation;

import com.cohort.api.exception.InvalidFileException;
import com.cohort.api.service.resume.storage.FileUpload;
import java.util.Locale;
import java.util.Set;

/** Stage 2 of the validation chain: the declared Content-Type — still entirely client-supplied, still trivially forgeable, but a second, independent lie the attacker has to get right. */
public class MimeTypeValidationHandler extends UploadValidationHandler {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    @Override
    protected void doValidate(FileUpload upload) {
        String contentType = upload.contentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidFileException("Unsupported content type: " + contentType);
        }
    }
}
