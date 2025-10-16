package com.intelliquiz.backend.repository;

import com.intelliquiz.backend.model.RefreshToken;
import com.intelliquiz.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Transactional
    int deleteByUser(User user);

    Optional<RefreshToken> findByUser(User user);
}
