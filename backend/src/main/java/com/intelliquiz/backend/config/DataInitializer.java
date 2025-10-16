package com.intelliquiz.backend.config;

import com.intelliquiz.backend.model.ERole;
import com.intelliquiz.backend.model.Role;
import com.intelliquiz.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        for (ERole eRole : ERole.values()) {
            roleRepository.findByName(eRole)
                    .orElseGet(() -> {
                        Role newRole = new Role(null, eRole);
                        roleRepository.save(newRole);
                        System.out.println("✅ Seeded role: " + eRole);
                        return newRole;
                    });
        }
    }
}
