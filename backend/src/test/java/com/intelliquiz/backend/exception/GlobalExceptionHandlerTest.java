package com.intelliquiz.backend.exception;

import com.intelliquiz.backend.controller.AuthController;
import com.intelliquiz.backend.repository.RoleRepository;
import com.intelliquiz.backend.repository.UserRepository;
import com.intelliquiz.backend.security.jwt.JwtUtils;
import com.intelliquiz.backend.security.services.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ✅ Unit test for GlobalExceptionHandler + AuthController validation
 * This test isolates AuthController and mocks all external dependencies.
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = AuthController.class)
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock all dependencies injected into AuthController
    @MockBean private AuthenticationManager authenticationManager;
    @MockBean private UserRepository userRepository;
    @MockBean private RoleRepository roleRepository;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private RefreshTokenService refreshTokenService;

    @Test
    void whenInvalidSignup_thenValidationErrorsReturned() throws Exception {
        // Prepare invalid JSON: missing username + invalid email + empty password
        String invalidBody = "{\"username\":\"\",\"email\":\"bad\",\"password\":\"\"}";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())       // GlobalExceptionHandler returns 400
                .andExpect(jsonPath("$.username").exists()) // Field-level error from validation
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").exists());
    }
}
