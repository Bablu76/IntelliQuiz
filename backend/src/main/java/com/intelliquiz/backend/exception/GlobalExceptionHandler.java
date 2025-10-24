package com.intelliquiz.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ✅ 1️⃣ Validation Errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        log.debug("Validation errors: {}", errors);
        return ResponseEntity.badRequest().body(Map.of(
                "errorType", "ValidationError",
                "details", errors
        ));
    }

    // ✅ 2️⃣ Token Refresh Exception
    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<?> handleTokenRefreshException(TokenRefreshException ex) {
        log.warn("Token refresh error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "errorType", "TokenRefreshError",
                "message", ex.getMessage()
        ));
    }

    // ✅ 3️⃣ Bad Request
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "errorType", "BadRequest",
                "message", ex.getMessage()
        ));
    }

    // ✅ 4️⃣ Access Denied
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "errorType", "AccessDenied",
                "message", ex.getMessage()
        ));
    }

    // ✅ 5️⃣ Entity Not Found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "errorType", "NotFound",
                "message", ex.getMessage()
        ));
    }

    // ✅ 6️⃣ Generic RuntimeException (merged)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        log.error("Runtime exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "errorType", "RuntimeError",
                "message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred"
        ));
    }

    // ✅ 7️⃣ Catch-all Exception fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAll(Exception ex) {
        log.error("Unhandled exception caught globally: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "errorType", "ServerError",
                "message", "Internal Server Error"
        ));
    }

    // ✅ 8️⃣ Handle file upload too large
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleFileTooLarge(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        log.warn("File upload too large: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
                "errorType", "FileTooLarge",
                "message", "Uploaded file exceeds allowed size limit"
        ));
    }




}
