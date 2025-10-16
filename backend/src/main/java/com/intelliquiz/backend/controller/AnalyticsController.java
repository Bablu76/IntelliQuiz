package com.intelliquiz.backend.controller;

import com.intelliquiz.backend.model.QuizAttempt;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.repository.UserRepository;
import com.intelliquiz.backend.security.services.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 📊 AnalyticsController
 * Generates real analytics data from QuizAttemptRepository.
 * Uses SLF4J logging for structured, production-grade monitoring.
 */
@RestController
@RequestMapping("/analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    public AnalyticsController(AnalyticsService analyticsService, UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
    }

    /**
     * Get analytics data for a student (real data).
     */
    @GetMapping("/student/{id}")
    public ResponseEntity<Map<String, Object>> getStudentAnalytics(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();

        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            log.warn("⚠️ No user found with ID {}", id);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No user found",
                    "studentId", id,
                    "accuracy", "0%",
                    "totalQuizzes", 0,
                    "badges", List.of("Unregistered User"),
                    "trend", List.of()
            ));
        }

        User user = optionalUser.get();
        List<QuizAttempt> attempts = analyticsService.getAttemptsByUser(user);

        // ✅ DEMO FALLBACK — show mock data if student has no attempts
        if (attempts.isEmpty()) {
            log.info("📊 [STUDENT_ANALYTICS-DEMO] No quiz attempts found for user {} → returning mock data", id);
            Map<String, Object> demoAnalytics = Map.of(
                    "studentId", id,
                    "accuracy", "78%",
                    "totalQuizzes", 5,
                    "badges", List.of("Consistent Performer", "Fast Learner"),
                    "trend", List.of(65, 70, 75, 80, 85)
            );
            return ResponseEntity.ok(demoAnalytics);
        }

        // 🧮 Real analytics path
        double avgScore = analyticsService.calculateAverageScore(user);
        int totalQuizzes = attempts.size();

        // Last 5 scores (trend)
        List<Integer> trend = attempts.stream()
                .sorted(Comparator.comparing(QuizAttempt::getAttemptedAt))
                .map(QuizAttempt::getScore)
                .skip(Math.max(0, attempts.size() - 5))
                .collect(Collectors.toList());

        // Assign badges
        List<String> badges = new ArrayList<>();
        if (avgScore >= 80) badges.add("Sharp Thinker");
        if (avgScore >= 90) badges.add("Quiz Master");
        if (totalQuizzes >= 10) badges.add("Persistent Learner");
        if (badges.isEmpty()) badges.add("Rookie");

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("studentId", id);
        analytics.put("accuracy", String.format("%.0f%%", avgScore));
        analytics.put("totalQuizzes", totalQuizzes);
        analytics.put("badges", badges);
        analytics.put("trend", trend);

        log.info("📈 [STUDENT_ANALYTICS] User ID={} | AvgScore={} | Quizzes={} | Duration={}ms",
                id, avgScore, totalQuizzes, System.currentTimeMillis() - startTime);

        return ResponseEntity.ok(analytics);
    }


    /**
     * Get leaderboard data for a classroom (real + mock hybrid).
     * Later this can pull from a ClassroomRepository.
     */
    @GetMapping("/classroom/{id}")
    public ResponseEntity<List<Map<String, Object>>> getClassroomLeaderboard(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();

        // For MVP: list all users with their avg scores (mock classroom)
        List<User> users = userRepository.findAll();

        List<Map<String, Object>> leaderboard = users.stream()
                .map(user -> {
                    // compute average and convert safely to int (rounded)
                    double avg = analyticsService.calculateAverageScore(user);
                    int scoreInt = (int) Math.round(avg);

                    Map<String, Object> entry = new HashMap<>();
                    entry.put("studentName", user.getUsername());
                    entry.put("score", scoreInt);
                    return entry;
                })
                // sort descending by score
                .sorted(Comparator.<Map<String, Object>>comparingInt(m -> (Integer) m.get("score")).reversed())
                .limit(10)
                .collect(Collectors.toList());


        log.info("🏫 [LEADERBOARD] Classroom ID={} | Entries={} | Duration={}ms",
                id, leaderboard.size(), System.currentTimeMillis() - startTime);

        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Example error case (for validation)
     */
    @GetMapping("/error-demo/{id}")
    public ResponseEntity<?> simulateError(@PathVariable Long id) {
        try {
            if (id < 0) throw new IllegalArgumentException("Invalid student ID: " + id);
            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (Exception e) {
            log.error("❌ [ANALYTICS_ERROR] ID={} | Message={}", id, e.getMessage(), e);
            throw e; // handled by GlobalExceptionHandler
        }
    }
}
