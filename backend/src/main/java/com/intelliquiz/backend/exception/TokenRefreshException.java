package com.intelliquiz.backend.exception;

/**
 * Exception used when refresh token operations fail.
 */
public class TokenRefreshException extends RuntimeException {
    public TokenRefreshException(String token, String msg) {
        super(String.format("Failed for [%s] : %s", token, msg));
    }
}
