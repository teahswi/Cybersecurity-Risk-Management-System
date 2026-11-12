package com.cybershield.storage;

import com.cybershield.exception.FileStorageException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores evidence files on the local disk in the directory configured by UPLOAD_DIR
 * (cybershield.upload-dir). The directory is outside src/main/resources/static, so files are never
 * publicly reachable; they are only served through access-controlled download endpoints.
 *
 * Security checks: max 10 MB, allowed extensions (pdf, png, jpg, jpeg, txt), declared content type must
 * match the extension, file signature ("magic bytes") must match, server-generated UUID file names,
 * and path traversal prevention on every read and write.
 */
@Service
public class FileStorageService {

    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    /** extension -> canonical MIME type */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "txt", "text/plain");

    private final Path root;

    public FileStorageService(@Value("${cybershield.upload-dir:./uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory " + root, e);
        }
    }

    public Path getRoot() {
        return root;
    }

    /** Validates and saves an uploaded file inside the given sub-directory (e.g. "remediation"). */
    public StoredFile store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Please choose a file to upload.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("The file is larger than the 10 MB limit.");
        }
        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        String mimeType = ALLOWED_TYPES.get(extension);
        if (mimeType == null) {
            throw new FileStorageException("Only PDF, PNG, JPG/JPEG and TXT files are allowed.");
        }
        validateDeclaredContentType(file.getContentType(), mimeType);
        byte[] header = readHeader(file);
        if (!signatureMatches(extension, header)) {
            throw new FileStorageException("The file content does not match its ." + extension + " extension.");
        }

        String storedName = UUID.randomUUID() + "." + extension;
        Path directory = resolveInsideRoot(subDirectory);
        Path target = directory.resolve(storedName).normalize();
        ensureInsideRoot(target);
        try {
            Files.createDirectories(directory);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new FileStorageException("The file could not be saved. Please try again.");
        }
        String relativePath = root.relativize(target).toString().replace('\\', '/');
        return new StoredFile(originalName, storedName, relativePath, mimeType, file.getSize());
    }

    /** Loads a stored file for download. The relative path always comes from the database, never from the URL. */
    public Resource load(String relativePath) {
        Path file = root.resolve(relativePath).normalize();
        ensureInsideRoot(file);
        if (!Files.isRegularFile(file)) {
            throw new FileStorageException("The evidence file is missing from the server.");
        }
        return new FileSystemResource(file);
    }

    public void delete(String relativePath) {
        Path file = root.resolve(relativePath).normalize();
        ensureInsideRoot(file);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new FileStorageException("The file could not be deleted.");
        }
    }

    /** Used by the demo data loader to create small sample evidence text files. Never overwrites a file. */
    public void writeTextFileIfMissing(String relativePath, String content) {
        Path file = root.resolve(relativePath).normalize();
        ensureInsideRoot(file);
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, content, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new FileStorageException("Could not write demo file " + relativePath);
        }
    }

    public boolean isWritable() {
        return Files.isDirectory(root) && Files.isWritable(root);
    }

    // ---------------------------------------------------------------- helpers

    static String sanitizeOriginalName(String name) {
        if (name == null || name.isBlank()) {
            throw new FileStorageException("The uploaded file has no name.");
        }
        // keep only the last path segment (browsers on Windows may send "C:\\folder\\file.pdf")
        String cleaned = name.replace('\\', '/');
        cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1).trim();
        if (cleaned.isEmpty() || cleaned.contains("..")) {
            throw new FileStorageException("Invalid file name.");
        }
        cleaned = cleaned.replaceAll("[^A-Za-z0-9._ ()-]", "_");
        return cleaned.length() > 200 ? cleaned.substring(cleaned.length() - 200) : cleaned;
    }

    static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw new FileStorageException("The file must have a .pdf, .png, .jpg, .jpeg or .txt extension.");
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void validateDeclaredContentType(String declared, String expected) {
        if (declared == null || declared.isBlank()) {
            return; // some clients do not send a content type - the signature check below still applies
        }
        String normalized = declared.toLowerCase(Locale.ROOT).split(";")[0].trim();
        if ("image/jpg".equals(normalized) || "image/pjpeg".equals(normalized)) {
            normalized = "image/jpeg";
        }
        boolean genericBinary = "application/octet-stream".equals(normalized);
        if (!genericBinary && !normalized.equals(expected)) {
            throw new FileStorageException("The file type (" + normalized + ") does not match its extension.");
        }
    }

    private byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(8192);
        } catch (IOException e) {
            throw new FileStorageException("The file could not be read.");
        }
    }

    static boolean signatureMatches(String extension, byte[] header) {
        return switch (extension) {
            case "pdf" -> startsWith(header, new byte[]{'%', 'P', 'D', 'F', '-'});
            case "png" -> startsWith(header, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
            case "jpg", "jpeg" -> startsWith(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "txt" -> isPlainText(header);
            default -> false;
        };
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPlainText(byte[] data) {
        for (byte b : data) {
            if (b == 0) {
                return false; // binary files contain NUL bytes
            }
        }
        return true;
    }

    private Path resolveInsideRoot(String subDirectory) {
        Path dir = root.resolve(subDirectory).normalize();
        ensureInsideRoot(dir);
        return dir;
    }

    private void ensureInsideRoot(Path path) {
        if (!path.toAbsolutePath().normalize().startsWith(root)) {
            throw new FileStorageException("Invalid file location.");
        }
    }
}
