package com.cohort.api.service.resume.storage.validation;

import com.cohort.api.exception.InvalidFileException;
import com.cohort.api.service.resume.storage.FileUpload;

public class MagicByteValidationHandler extends UploadValidationHandler {
    private static final byte[] PDF_SIGNATURE = {0x25, 0x50, 0x44, 0x46};
    private static final byte[] ZIP_SIGNATURE = {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] OLE2_SIGNATURE = {
        (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };

    @Override
    protected void doValidate(FileUpload upload) {
        byte[] content = upload.content();
        if (!startsWith(content, PDF_SIGNATURE) && !startsWith(content, ZIP_SIGNATURE) && !startsWith(content, OLE2_SIGNATURE)) {
            throw new InvalidFileException(
                    "The file is not a PDF, DOC or DOCX. Its contents were checked, not just its name.");
        }
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content == null || content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
