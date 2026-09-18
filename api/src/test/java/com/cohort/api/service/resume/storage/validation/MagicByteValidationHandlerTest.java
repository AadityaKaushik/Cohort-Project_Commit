package com.cohort.api.service.resume.storage.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cohort.api.exception.InvalidFileException;
import com.cohort.api.service.resume.storage.FileUpload;
import org.junit.jupiter.api.Test;

/** Exercises stage 3 of the chain in isolation — "independently testable" (ARCHITECTURE_REVISION_JAVA.md §4.1) is a claim this test backs up. */
class MagicByteValidationHandlerTest {

    private final MagicByteValidationHandler handler = new MagicByteValidationHandler();

    @Test
    void acceptsRealPdfBytes() {
        FileUpload upload = new FileUpload("r.pdf", "application/pdf", new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D});
        assertThatCode(() -> handler.handle(upload)).doesNotThrowAnyException();
    }

    @Test
    void acceptsRealDocxBytesEvenThoughItsAZipContainer() {
        FileUpload upload = new FileUpload("r.docx", "application/vnd...", new byte[] {0x50, 0x4B, 0x03, 0x04});
        assertThatCode(() -> handler.handle(upload)).doesNotThrowAnyException();
    }

    @Test
    void rejectsWindowsExecutableBytesRegardlessOfClaimedMetadata() {
        byte[] mzHeader = {0x4D, 0x5A, (byte) 0x90, 0x00};
        FileUpload upload = new FileUpload("resume.pdf", "application/pdf", mzHeader);

        assertThatThrownBy(() -> handler.handle(upload)).isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsAnEmptyFile() {
        FileUpload upload = new FileUpload("resume.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> handler.handle(upload)).isInstanceOf(InvalidFileException.class);
    }
}
