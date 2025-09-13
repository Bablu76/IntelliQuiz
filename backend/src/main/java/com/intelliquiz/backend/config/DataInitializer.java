package com.intelliquiz.backend.config;

import com.intelliquiz.backend.model.*;
import com.intelliquiz.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(null, ERole.ROLE_ADMIN));
            roleRepository.save(new Role(null, ERole.ROLE_TEACHER));
            roleRepository.save(new Role(null, ERole.ROLE_STUDENT));
            System.out.println("Seeded roles");
        }
    }
}
