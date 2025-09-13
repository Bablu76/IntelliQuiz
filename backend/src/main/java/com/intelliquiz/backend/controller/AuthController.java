package com.intelliquiz.backend.controller;

import com.intelliquiz.backend.model.*;
import com.intelliquiz.backend.payload.request.*;
import com.intelliquiz.backend.payload.response.*;
import com.intelliquiz.backend.repository.*;
import com.intelliquiz.backend.security.jwt.JwtUtils;
import com.intelliquiz.backend.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(auth -> auth.getAuthority())
                .toList(); // modern way

        return ResponseEntity.ok(
                new JwtResponse(jwt,
                        userDetails.getId(),
                        userDetails.getUsername(),
                        userDetails.getEmail(),
                        roles)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
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

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}
