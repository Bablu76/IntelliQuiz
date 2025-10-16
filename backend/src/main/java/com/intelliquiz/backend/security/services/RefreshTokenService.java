package com.intelliquiz.backend.security.services;

import com.intelliquiz.backend.model.RefreshToken;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.repository.RefreshTokenRepository;
import com.intelliquiz.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.jwtRefreshExpirationMs:604800000}") // 7 days default
    private Long refreshTokenDurationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /** ✅ Find refresh token by token string */
    public Optional<RefreshToken> findByToken(String token) {
        log.debug("🔍 Searching refresh token in DB...");
        return refreshTokenRepository.findByToken(token);
    }

    /** ✅ Create or update refresh token for a user (transactional) */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Optional<RefreshToken> existingOpt = refreshTokenRepository.findByUser(user);
        RefreshToken token = existingOpt.orElse(new RefreshToken());

        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        RefreshToken saved = refreshTokenRepository.save(token);

        log.info("💾 Refresh token {} for user '{}' [expires: {}]",
                existingOpt.isPresent() ? "updated" : "created",
                user.getUsername(),
                token.getExpiryDate());

        return saved;
    }

    /** ✅ Verify and refresh expiry */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            log.warn("⚠️ Expired refresh token for user '{}' (expired at: {})",
                    token.getUser().getUsername(), token.getExpiryDate());
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token expired. Please sign in again.");
        }
        log.debug("✅ Valid refresh token for user '{}'", token.getUser().getUsername());
        return token;
    }

    /** ✅ Delete refresh token for a user safely */
    @Transactional
    public int deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        int deleted = refreshTokenRepository.deleteByUser(user);
        log.info("🗑️ Deleted {} refresh token(s) for user '{}'", deleted, user.getUsername());
        return deleted;
    }
}
