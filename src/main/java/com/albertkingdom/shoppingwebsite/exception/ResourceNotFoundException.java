package com.albertkingdom.shoppingwebsite.exception;

/**
 * Signals that a requested resource (product / order / user / ...) does not
 * exist. Translated to HTTP 404 by ApiExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
