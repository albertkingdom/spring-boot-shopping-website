package com.albertkingdom.shoppingwebsite.exception;

/**
 * Signals a uniqueness / state conflict. Translated to HTTP 409 by
 * ApiExceptionHandler.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
