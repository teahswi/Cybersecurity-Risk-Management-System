package com.cybershield.exception;

/** Thrown when a requested record does not exist. Rendered as the 404 page. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String recordName, Object id) {
        return new ResourceNotFoundException(recordName + " " + id + " was not found.");
    }
}
