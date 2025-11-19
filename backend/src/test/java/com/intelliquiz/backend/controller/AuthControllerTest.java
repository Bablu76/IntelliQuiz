package com.intelliquiz.backend.controller;

import com.intelliquiz.backend.model.*;
import com.intelliquiz.backend.payload.request.LoginRequest;
import com.intelliquiz.backend.payload.request.SignupRequest;
import com.intelliquiz.backend.payload.response.JwtResponse;
import com.intelliquiz.backend.payload.response.MessageResponse;
import com.intelliquiz.backend.repository.*;
import com.intelliquiz.backend.security.jwt.JwtUtils;
import com.intelliquiz.backend.service.RefreshTokenService;
import com.intelliquiz.backend.service.UserDetailsImpl;
import org.springframework.test.util.ReflectionTestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        // Manually set the autowired field not covered by constructor
        ReflectionTestUtils.setField(authController, "refreshTokenService", refreshTokenService);
    }


    // ✅ Test Login Success
    @Test
    void testLogin_SuccessfulAuthentication() {
        LoginRequest request = new LoginRequest();
        request.setUsername("student1");
        request.setPassword("password");

        Authentication mockAuth = mock(Authentication.class);
        UserDetailsImpl mockUser = new UserDetailsImpl(1L, "student1", "student@mail.com", "encoded", List.of());
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(jwtUtils.generateJwtToken(any())).thenReturn("fake.jwt.token");

        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh-token");
        when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(rt);

        ResponseEntity<?> response = authController.authenticateUser(request);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof JwtResponse);
        JwtResponse jwt = (JwtResponse) response.getBody();
        assertEquals("fake.jwt.token", jwt.getToken());
    }

    // 🚫 Test Login with Bad Credentials
    @Test
    void testLogin_BadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("invalid");
        request.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid"));

        ResponseEntity<?> response = authController.authenticateUser(request);
        assertEquals(401, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof MessageResponse);
        assertEquals("Invalid username or password",
                ((MessageResponse) response.getBody()).getMessage());
    }

    // ✅ Test Register Success
    @Test
    void testRegister_Success() {
        SignupRequest req = new SignupRequest();
        req.setUsername("newstudent");
        req.setEmail("new@student.com");
        req.setPassword("12345");
        req.setRole(Set.of("student"));

        when(userRepository.existsByUsername("newstudent")).thenReturn(false);
        when(userRepository.existsByEmail("new@student.com")).thenReturn(false);

        Role studentRole = new Role();
        studentRole.setName(ERole.ROLE_STUDENT);
        when(roleRepository.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(encoder.encode(anyString())).thenReturn("encoded123");

        ResponseEntity<MessageResponse> resp = authController.registerUser(req);

        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("User registered successfully!", resp.getBody().getMessage());
    }

    // 🚫 Test Register with Existing Username
    @Test
    void testRegister_UsernameExists() {
        SignupRequest req = new SignupRequest();
        req.setUsername("existing");
        req.setEmail("test@mail.com");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        ResponseEntity<MessageResponse> resp = authController.registerUser(req);
        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().getMessage().contains("Username is already taken"));
    }
}
