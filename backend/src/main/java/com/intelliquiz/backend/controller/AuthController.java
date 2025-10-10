package com.intelliquiz.backend.controller;

import com.intelliquiz.backend.model.*;
import com.intelliquiz.backend.payload.request.*;
import com.intelliquiz.backend.payload.response.*;
import com.intelliquiz.backend.repository.*;
import com.intelliquiz.backend.security.jwt.JwtUtils;
import com.intelliquiz.backend.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@CrossOrigin(origins = "http://localhost:5173") // ← ADD THIS FOR CORS
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    // Predefine role mapping (case-insensitive)
    private static final Map<String, ERole> ROLE_MAP = Map.of(
            "admin", ERole.ROLE_ADMIN,
            "teacher", ERole.ROLE_TEACHER,
            "student", ERole.ROLE_STUDENT
    );

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Log the login attempt
            System.out.println("🔐 Login attempt for user: " + loginRequest.getUsername());

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            System.out.println("✅ Authentication successful for: " + loginRequest.getUsername());

            // Generate JWT token
            String jwt = jwtUtils.generateJwtToken(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            List<String> roles = userDetails.getAuthorities()
                    .stream()
                    .map(auth -> auth.getAuthority())
                    .toList();

            System.out.println("🎫 Token generated successfully");

            // Return success response with "token" key for frontend compatibility
            JwtResponse response = new JwtResponse(
                    jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    roles
            );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            // Wrong username or password - return 401
            System.out.println("❌ Bad credentials for user: " + loginRequest.getUsername());

            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid username or password");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (DisabledException e) {
            // Account is disabled
            System.out.println("❌ Account disabled for user: " + loginRequest.getUsername());

            Map<String, String> error = new HashMap<>();
            error.put("message", "Account is disabled");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (LockedException e) {
            // Account is locked
            System.out.println("❌ Account locked for user: " + loginRequest.getUsername());

            Map<String, String> error = new HashMap<>();
            error.put("message", "Account is locked");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (AuthenticationException e) {
            // Other authentication errors
            System.out.println("❌ Authentication failed: " + e.getMessage());

            Map<String, String> error = new HashMap<>();
            error.put("message", "Authentication failed: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (Exception e) {
            // Unexpected errors
            System.out.println("❌ Unexpected error during login: " + e.getMessage());
            e.printStackTrace();

            Map<String, String> error = new HashMap<>();
            error.put("message", "An error occurred during login");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        try {
            System.out.println("📝 Registration attempt for user: " + signUpRequest.getUsername());

            if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                System.out.println("❌ Username already taken: " + signUpRequest.getUsername());
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
            }

            if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                System.out.println("❌ Email already in use: " + signUpRequest.getEmail());
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
            }

            // Create new user
            User user = new User();
            user.setUsername(signUpRequest.getUsername());
            user.setEmail(signUpRequest.getEmail());
            user.setPassword(encoder.encode(signUpRequest.getPassword()));

            Set<Role> roles = new HashSet<>();
            Set<String> strRoles = Optional.ofNullable(signUpRequest.getRole()).orElse(Set.of("student"));

            for (String roleStr : strRoles) {
                ERole eRole = ROLE_MAP.getOrDefault(roleStr.toLowerCase(), ERole.ROLE_STUDENT);
                Role roleEntity = roleRepository.findByName(eRole)
                        .orElseThrow(() -> new RuntimeException("Error: Role not found: " + roleStr));
                roles.add(roleEntity);
            }

            user.setRoles(roles);
            userRepository.save(user);

            System.out.println("✅ User registered successfully: " + signUpRequest.getUsername());

            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));

        } catch (Exception e) {
            System.out.println("❌ Registration error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Registration failed: " + e.getMessage()));
        }
    }
}