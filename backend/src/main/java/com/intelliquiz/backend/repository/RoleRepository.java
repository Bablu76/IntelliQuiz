package com.intelliquiz.backend.repository;

import com.intelliquiz.backend.model.ERole;
import com.intelliquiz.backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Long> {
    Optional<Role> findByName(ERole name);
}
