package com.intelliquiz.backend.controller;

import com.intelliquiz.backend.repository.UserRepository;
import com.intelliquiz.backend.security.services.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    public AnalyticsController(AnalyticsService analyticsService, UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
    }

    // ==============================
    // 📊 Student Analytics Endpoint
    // ==============================

    /**
     * Returns analytics data (average, accuracy, badges, points, trend)
     * for a given student ID.
     */
    @GetMapping("/student/{id}")
    public ResponseEntity<?> getStudentAnalytics(@PathVariable Long id) {
        log.info("📊 GET /analytics/student/{}", id);
        try {
            if (!userRepository.existsById(id)) {
                log.warn("⚠️ No user found with ID {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found", "userId", id));
            }

            Map<String, Object> analytics = analyticsService.getStudentAnalytics(id);
            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            log.error("❌ Error fetching analytics for user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error while fetching analytics data"));
        }
    }

    // ==============================
    // 🏆 Global Leaderboard Endpoint
    // ==============================

    /**
     * Returns leaderboard data — top N users sorted by points.
     * Query param `limit` defines number of entries (default 10).
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(@RequestParam Optional<Integer> limit) {
        int topLimit = limit.orElse(10);
        log.info("🏆 GET /analytics/leaderboard?limit={}", topLimit);
        try {
            List<Map<String, Object>> leaderboard = analyticsService.getLeaderboard(topLimit);
            return ResponseEntity.ok(leaderboard);
        } catch (Exception e) {
            log.error("❌ Error generating leaderboard: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error while generating leaderboard"));
        }
    }

    // ==============================
    // 🏫 Classroom Leaderboard (Demo)
    // ==============================

    /**
     * Retained demo endpoint — placeholder until Classroom entity is active.
     */
    @GetMapping("/classroom/{id}")
    public ResponseEntity<Map<String, Object>> getClassroomAnalytics(@PathVariable Long id) {
        log.info("🏫 GET /analytics/classroom/{}", id);
        return ResponseEntity.ok(analyticsService.getClassroomAnalytics(id));
    }

    // ==============================
    // 🧪 Test / Error Simulation
    // ==============================

    @GetMapping("/error-demo/{id}")
    public ResponseEntity<?> simulateError(@PathVariable Long id) {
        log.info("🧪 Simulating analytics error for ID {}", id);
        if (id < 0) {
            throw new IllegalArgumentException("Invalid student ID: " + id);
        }
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}
