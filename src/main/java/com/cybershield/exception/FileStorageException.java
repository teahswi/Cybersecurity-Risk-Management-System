package com.cybershield.exception;

/** Thrown when an uploaded file is invalid or cannot be stored. The message is safe to show. */
public class FileStorageException extends BusinessRuleException {

    public FileStorageException(String message) {
        super(message);
    }
}
