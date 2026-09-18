package com.cohort.api.service.resume.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cohort.api.exception.FileTooLargeException;
import com.cohort.api.exception.InvalidFileException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Decorator + Chain of Responsibility (ARCHITECTURE_REVISION_JAVA.md §4.1).
 * The delegate is mocked so these tests prove two things independently:
 * (1) a bad upload never reaches the underlying storage at all, and (2) the
 * mandatory case for this phase — a renamed executable is caught by
 * magic-byte sniffing specifically, not by the (easily forged) extension or
 * Content-Type checks that run before it.
 */
class ValidatingFileStorageTest {

    private static final byte[] REAL_PDF_BYTES = "%PDF-1.4\n%fake but valid header".getBytes(StandardCharsets.UTF_8);
    // A Windows PE executable's real header: "MZ" followed by the rest of the DOS stub.
    private static final byte[] WINDOWS_EXECUTABLE_BYTES =
            new byte[] {0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};

    private FileStorageStrategy delegate;
    private ValidatingFileStorage validatingFileStorage;

    @BeforeEach
    void setUp() {
        delegate = mock(FileStorageStrategy.class);
        validatingFileStorage = new ValidatingFileStorage(delegate);
    }

    @Test
    void aGenuinePdfIsAcceptedAndForwardedToTheDelegate() {
        UUID ownerId = UUID.randomUUID();
        FileUpload upload = new FileUpload("resume.pdf", "application/pdf", REAL_PDF_BYTES);
        StoredFile expected = new StoredFile("key", "resume.pdf", "application/pdf", REAL_PDF_BYTES.length);
        when(delegate.store(ownerId, upload)).thenReturn(expected);

        StoredFile result = validatingFileStorage.store(ownerId, upload);

        assertThat(result).isEqualTo(expected);
        verify(delegate).store(ownerId, upload);
    }

    @Test
    void aWindowsExecutableRenamedToPdfWithAForgedContentTypeIsRejectedByMagicByteSniffing() {
        // Passes stage 1 (extension is literally ".pdf") and stage 2 (Content-Type
        // header is forged to "application/pdf") — only the magic-byte stage,
        // which inspects the file's real header bytes, catches this.
        FileUpload disguisedExecutable = new FileUpload("resume.pdf", "application/pdf", WINDOWS_EXECUTABLE_BYTES);

        assertThatThrownBy(() -> validatingFileStorage.store(UUID.randomUUID(), disguisedExecutable))
                .isInstanceOf(InvalidFileException.class);

        verify(delegate, never()).store(any(), any());
    }

    @Test
    void aDisallowedExtensionIsRejectedBeforeEverInspectingContent() {
        FileUpload upload = new FileUpload("resume.exe", "application/pdf", REAL_PDF_BYTES);

        assertThatThrownBy(() -> validatingFileStorage.store(UUID.randomUUID(), upload))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("extension");

        verify(delegate, never()).store(any(), any());
    }

    @Test
    void aForgedContentTypeThatDoesNotMatchAnAllowedMimeTypeIsRejected() {
        FileUpload upload = new FileUpload("resume.pdf", "application/x-msdownload", REAL_PDF_BYTES);

        assertThatThrownBy(() -> validatingFileStorage.store(UUID.randomUUID(), upload))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("content type");

        verify(delegate, never()).store(any(), any());
    }

    @Test
    void aFileOverTheFiveMegabyteCapIsRejectedWithoutRunningTheValidationChain() {
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        FileUpload upload = new FileUpload("resume.pdf", "application/pdf", oversized);

        assertThatThrownBy(() -> validatingFileStorage.store(UUID.randomUUID(), upload))
                .isInstanceOf(FileTooLargeException.class);

        verify(delegate, never()).store(any(), any());
    }

    @Test
    void loadAndDeletePassStraightThroughWithoutValidation() {
        validatingFileStorage.load("some-key");
        validatingFileStorage.delete("some-key");

        verify(delegate).load("some-key");
        verify(delegate).delete("some-key");
    }
}
