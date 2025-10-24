package com.intelliquiz.backend.exception;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;

import static org.mockito.Mockito.*;

public class MockValidationHelper {
    public static BindingResult mockBindingResult() {
        BindingResult result = mock(BindingResult.class);
        // lightweight fake validation error
        FieldError fieldError = new FieldError("mockBean", "topic", "must not be empty");
        when(result.getFieldErrors()).thenReturn(List.of(fieldError));
        return result;
    }
}
