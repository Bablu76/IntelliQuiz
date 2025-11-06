package com.intelliquiz.backend.exception;

/**
 * Simple runtime exception to represent a 400-level bad request.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException() { super(); }
    public BadRequestException(String message) { super(message); }
    public BadRequestException(String message, Throwable cause) { super(message, cause); }
}
