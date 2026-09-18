package com.cohort.api.service.resume.storage.validation;

import com.cohort.api.exception.InvalidFileException;
import com.cohort.api.service.resume.storage.FileUpload;

/**
 * Stage 3, and the one that actually matters: sniffs the file's own header
 * bytes rather than trusting anything the client claimed. A renamed
 * executable (e.g. a Windows PE binary, header {@code 4D 5A} / "MZ",
 * re-extensioned to {@code .pdf} with a forged {@code Content-Type}) sails
 * through stages 1 and 2 and is only caught here — this is the specific
 * scenario the risk table ("malicious resume upload") and the mandatory
 * test for this phase both call out.
 */
public class MagicByteValidationHandler extends UploadValidationHandler {

    // %PDF
    private static final byte[] PDF_SIGNATURE = {0x25, 0x50, 0x44, 0x46};
    // PK\x03\x04 — a .docx is a zip archive under the hood.
    private static final byte[] ZIP_SIGNATURE = {0x50, 0x4B, 0x03, 0x04};
    // Legacy binary .doc (OLE2 compound file) signature.
    private static final byte[] OLE2_SIGNATURE = {
        (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };

    @Override
    protected void doValidate(FileUpload upload) {
        byte[] content = upload.content();
        if (!startsWith(content, PDF_SIGNATURE) && !startsWith(content, ZIP_SIGNATURE) && !startsWith(content, OLE2_SIGNATURE)) {
            throw new InvalidFileException(
                    "File content does not match any supported resume format (PDF, DOC, DOCX) — "
                            + "the file's actual bytes were inspected and matched none of the expected signatures, "
                            + "regardless of its extension or declared content type.");
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
