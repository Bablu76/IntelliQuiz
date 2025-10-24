package com.intelliquiz.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidation_returns400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error = mock(FieldError.class);
        when(error.getField()).thenReturn("topic");
        when(error.getDefaultMessage()).thenReturn("must not be empty");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error));
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<?> response = handler.handleValidation(ex);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) response.getBody();
        assertThat(map.get("errorType")).isEqualTo("ValidationError");

        // ✅ Explicitly cast "details" to a proper map to avoid wildcard capture issue
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) map.get("details");
        assertThat(details).containsKey("topic");
        assertThat(details.get("topic")).isEqualTo("must not be empty");
    }




    @Test
    void handleTokenRefresh_returns400() {
        TokenRefreshException ex = new TokenRefreshException("token123", "Refresh token expired");
        ResponseEntity<?> response = handler.handleTokenRefreshException(ex);
        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody().toString()).contains("TokenRefreshError");
    }

    @Test
    void handleBadRequest_returns400() {
        com.intelliquiz.backend.exception.BadRequestException ex = new BadRequestException("Only PDF files are allowed");
        ResponseEntity<?> response = handler.handleBadRequest(ex);
        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody().toString()).contains("Only PDF files are allowed");
    }


    @Test
    void handleAccessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        ResponseEntity<?> response = handler.handleAccessDenied(ex);
        assertThat(response.getStatusCodeValue()).isEqualTo(403);
        assertThat(response.getBody().toString()).contains("Access denied");
    }

    @Test
    void handleEntityNotFound_returns404() {
        EntityNotFoundException ex = new EntityNotFoundException("Resource not found");
        ResponseEntity<?> response = handler.handleNotFound(ex);
        assertThat(response.getStatusCodeValue()).isEqualTo(404);
        assertThat(response.getBody().toString()).contains("Resource not found");
    }

    @Test
    void handleFileTooLarge_returns413() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10_000L);
        ResponseEntity<?> response = handler.handleFileTooLarge(ex);
        assertThat(response.getStatusCodeValue()).isEqualTo(413);
        assertThat(response.getBody().toString()).contains("FileTooLarge");
    }

    @Test
    void handleRuntime_returns500() {
        RuntimeException ex = new RuntimeException("Unexpected runtime");
        ResponseEntity<?> response = handler.handleRuntime(ex);
        assertThat(response.getStatusCodeValue()).isEqualTo(500);
        assertThat(response.getBody().toString()).contains("RuntimeError");
    }

    @Test
    void handleGenericException_returns500() {
        Exception ex = new Exception("Something went wrong");
        ResponseEntity<?> response = handler.handleAll(ex);
        assertThat(response.getStatusCodeValue()).isEqualTo(500);
        assertThat(response.getBody().toString()).contains("ServerError");
    }
}
