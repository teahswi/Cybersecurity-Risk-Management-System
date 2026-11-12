package com.cybershield.storage;

/** Metadata of a file saved by {@link FileStorageService}. */
public record StoredFile(String originalFilename, String storedFilename, String relativePath, String mimeType,
                         long size) {
}
