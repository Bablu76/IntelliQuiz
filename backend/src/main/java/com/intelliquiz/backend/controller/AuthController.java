package com.intelliquiz.backend.controller;

import com.intelliquiz.backend.exception.TokenRefreshException;
import com.intelliquiz.backend.model.*;
import com.intelliquiz.backend.payload.request.*;
import com.intelliquiz.backend.payload.response.*;
import com.intelliquiz.backend.repository.*;
import com.intelliquiz.backend.security.jwt.JwtUtils;
import com.intelliquiz.backend.security.services.RefreshTokenService;
import com.intelliquiz.backend.security.services.UserDetailsImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    // ✅ Centralized SLF4J logger
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    @Autowired
    private RefreshTokenService refreshTokenService;

    // Predefined role mapping
    private static final Map<String, ERole> ROLE_MAP = Map.of(
            "admin", ERole.ROLE_ADMIN,
            "teacher", ERole.ROLE_TEACHER,
            "student", ERole.ROLE_STUDENT
    );

    // 🔐 LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        log.info("🔐 Login attempt for user: {}", username);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );

            log.info("✅ Authentication successful for {}", username);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            log.debug("👤 Roles for {}: {}", username, roles);

            String jwt = jwtUtils.generateJwtToken(authentication);
            log.debug("🎫 Access token generated for {}", username);

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());
            log.debug("♻️ Refresh token created for {}", username);

            return ResponseEntity.ok(new JwtResponse(
                    jwt,
                    refreshToken.getToken(),
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    roles
            ));

        } catch (BadCredentialsException e) {
            log.warn("❌ Invalid credentials for {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Invalid username or password"));
        } catch (DisabledException e) {
            log.warn("❌ Account disabled for {}", username);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Account is disabled"));
        } catch (LockedException e) {
            log.warn("❌ Account locked for {}", username);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Account is locked"));
        } catch (AuthenticationException e) {
            log.error("❌ Authentication failure for {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Authentication failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Unexpected error during login for {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("An error occurred during login"));
        }
    }

    // 🧾 REGISTER
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        String username = signUpRequest.getUsername();
        String email = signUpRequest.getEmail();
        log.info("📝 Registration attempt for user: {}", username);

        try {
            if (userRepository.existsByUsername(username)) {
                log.warn("❌ Username '{}' already taken", username);
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Username is already taken!"));
            }

            if (userRepository.existsByEmail(email)) {
                log.warn("❌ Email '{}' already in use", email);
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Email is already in use!"));
            }

            User user = new User(username, email, encoder.encode(signUpRequest.getPassword()));

            Set<String> strRoles = Optional.ofNullable(signUpRequest.getRole())
                    .orElse(Set.of("student"));
            Set<Role> roles = new HashSet<>();

            for (String roleStr : strRoles) {
                ERole eRole = ROLE_MAP.getOrDefault(roleStr.toLowerCase(), ERole.ROLE_STUDENT);
                Role roleEntity = roleRepository.findByName(eRole)
                        .orElseThrow(() -> new RuntimeException("Error: Role not found - " + roleStr));
                roles.add(roleEntity);
            }

            user.setRoles(roles);
            userRepository.save(user);

            log.info("✅ User '{}' registered successfully with roles {}", username, roles);
            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));

        } catch (Exception e) {
            log.error("❌ Registration error for {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Registration failed: " + e.getMessage()));
        }
    }

    // 🔄 REFRESH TOKEN
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String requestRefreshToken = request.get("refreshToken");

        if (requestRefreshToken == null || requestRefreshToken.trim().isEmpty()) {
            log.warn("⚠️ Refresh token missing from request");
            throw new TokenRefreshException("null", "Refresh token is missing in the request body.");
        }

        log.info("♻️ Refresh token request received");

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    log.debug("✅ Valid refresh token for user: {}", user.getUsername());

                    refreshTokenService.deleteByUserId(user.getId());
                    log.debug("🧹 Old refresh token invalidated for user: {}", user.getUsername());

                    String newAccessToken = jwtUtils.generateTokenFromUsername(user.getUsername());
                    RefreshToken newRefresh = refreshTokenService.createRefreshToken(user.getId());

                    log.info("🔁 Issued new tokens for {}", user.getUsername());

                    return ResponseEntity.ok(Map.of(
                            "accessToken", newAccessToken,
                            "refreshToken", newRefresh.getToken()
                    ));
                })
                .orElseThrow(() -> new TokenRefreshException(
                        requestRefreshToken,
                        "Refresh token not found or expired. Please log in again."
                ));
    }

    // 🚪 LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        refreshTokenService.deleteByUserId(userId);
        log.info("🚪 User with ID {} logged out, refresh token(s) deleted", userId);
        return ResponseEntity.ok(Map.of("message", "User logged out successfully."));
    }
}
