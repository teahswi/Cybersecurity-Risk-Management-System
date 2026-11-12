package com.cybershield.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cybershield.exception.FileStorageException;
import com.cybershield.storage.FileStorageService;
import com.cybershield.storage.StoredFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/** Evidence upload validation: extension, content type, file signature, size and path traversal. */
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesValidTextFileWithGeneratedName() throws Exception {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        StoredFile stored = storage.store(new MockMultipartFile("file", "patch-report.txt", "text/plain",
                "Patched OK".getBytes(StandardCharsets.UTF_8)), "remediation");
        assertThat(stored.storedFilename()).endsWith(".txt").doesNotContain("patch-report");
        assertThat(stored.mimeType()).isEqualTo("text/plain");
        assertThat(Files.readString(tempDir.resolve(stored.relativePath()))).isEqualTo("Patched OK");
        assertThat(storage.load(stored.relativePath()).exists()).isTrue();
    }

    @Test
    void storesValidPdf() {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        StoredFile stored = storage.store(new MockMultipartFile("file", "evidence.pdf", "application/pdf",
                "%PDF-1.7 sample".getBytes(StandardCharsets.US_ASCII)), "remediation");
        assertThat(stored.mimeType()).isEqualTo("application/pdf");
    }

    @Test
    void rejectsDisallowedExtension() {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "script.exe", "application/octet-stream",
                new byte[]{1, 2, 3}), "remediation")).isInstanceOf(FileStorageException.class);
    }

    @Test
    void rejectsContentThatDoesNotMatchExtension() {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "fake.png", "image/png",
                "not really a png".getBytes(StandardCharsets.UTF_8)), "remediation"))
                .isInstanceOf(FileStorageException.class).hasMessageContaining("does not match");
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "doc.pdf", "image/png",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8)), "remediation"))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void rejectsEmptyAndOversizedFiles() {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]),
                "remediation")).isInstanceOf(FileStorageException.class);
        byte[] big = new byte[(int) FileStorageService.MAX_FILE_SIZE + 1];
        java.util.Arrays.fill(big, (byte) 'a');
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "big.txt", "text/plain", big),
                "remediation")).isInstanceOf(FileStorageException.class).hasMessageContaining("10 MB");
    }

    @Test
    void preventsPathTraversal() {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        assertThatThrownBy(() -> storage.load("../../etc/passwd")).isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "ok.txt", "text/plain",
                "x".getBytes(StandardCharsets.UTF_8)), "../outside")).isInstanceOf(FileStorageException.class);
        // a malicious original name is reduced to its last segment and never used on disk
        StoredFile stored = storage.store(new MockMultipartFile("file", "..\\..\\evil.txt", "text/plain",
                "x".getBytes(StandardCharsets.UTF_8)), "remediation");
        assertThat(stored.originalFilename()).isEqualTo("evil.txt");
        assertThat(tempDir.resolve(stored.relativePath()).normalize().startsWith(tempDir)).isTrue();
    }
}
