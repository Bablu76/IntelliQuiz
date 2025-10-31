package com.intelliquiz.backend.security.services;

import com.intelliquiz.backend.model.QuizAttempt;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.model.dto.TopicAnalyticsDTO;
import com.intelliquiz.backend.repository.QuizAttemptRepository;
import com.intelliquiz.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AnalyticsService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final UserRepository userRepository;

    public AnalyticsService(QuizAttemptRepository quizAttemptRepository,
                            UserRepository userRepository) {
        this.quizAttemptRepository = quizAttemptRepository;
        this.userRepository = userRepository;
    }

    // ✅ Existing: fetch all attempts by user
    public List<QuizAttempt> getAttemptsByUser(User user) {
        return quizAttemptRepository.findByUser(user);
    }

    // ✅ Enhanced: per-topic analytics
    public List<TopicAnalyticsDTO> getTopicAnalytics(Long userId) {
        try {
            List<Object[]> rows = quizAttemptRepository.findTopicAveragesByUser(userId);
            if (rows == null || rows.isEmpty()) {
                log.info("ℹ️ No topic data found for user {}", userId);
                return Collections.emptyList();
            }

            return rows.stream()
                    .map(r -> new TopicAnalyticsDTO(
                            (String) r[0],
                            r[1] != null ? Math.round(((Number) r[1]).doubleValue() * 100.0) / 100.0 : 0.0,
                            r[2] != null ? ((Number) r[2]).longValue() : 0L
                    ))
                    .sorted(Comparator.comparingDouble(TopicAnalyticsDTO::getAccuracy).reversed())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Failed to compute topic analytics for user {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ✅ New: weakest N topics
    public List<String> getWeakTopics(Long userId, int limit) {
        List<TopicAnalyticsDTO> list = getTopicAnalytics(userId);
        if (list.isEmpty()) return Collections.emptyList();

        return list.stream()
                .sorted(Comparator.comparingDouble(TopicAnalyticsDTO::getAccuracy))
                .limit(limit)
                .map(TopicAnalyticsDTO::getTopic)
                .collect(Collectors.toList());
    }


    // ✅ Utility: average score per user
    public double calculateAverageScore(User user) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByUser(user);
        return attempts.stream()
                .mapToInt(QuizAttempt::getScore)
                .average()
                .orElse(0.0);
    }

    /**
     * ✅ Enhanced: Returns full analytics metrics for a specific student.
     */
    public Map<String, Object> getStudentAnalytics(Long userId) {
        log.info("📊 Fetching analytics for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // ✅ Core stats
        Double avgScore = quizAttemptRepository.findAverageScore(userId);
        avgScore = (avgScore == null) ? 0.0 : Math.round(avgScore * 100.0) / 100.0;

        List<Integer> recentScores = quizAttemptRepository.findLastFiveScores(userId);
        Collections.reverse(recentScores); // chronological (oldest → newest)

        double accuracy = avgScore;
        int points = user.getPoints();

        List<String> badges = Arrays.stream(
                        Optional.ofNullable(user.getBadges()).orElse("")
                                .split(","))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        // ✅ Topic Analytics + Weak Topics
        List<TopicAnalyticsDTO> topicAnalytics = getTopicAnalytics(userId);
        List<String> weakTopics = getWeakTopics(userId,3);

        // ✅ Build unified response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", userId);
        response.put("averageScore", avgScore);
        response.put("accuracy", accuracy);
        response.put("trend", recentScores);
        response.put("points", points);
        response.put("badges", badges);
        response.put("topicAnalytics", topicAnalytics);
        response.put("weakTopics", weakTopics);

        log.info("✅ Analytics response built for user {}: {} topics, {} weak topics",
                userId, topicAnalytics.size(), weakTopics.size());
        return response;
    }

    /**
     * ✅ Leaderboard - unchanged but hardened
     */
    public List<Map<String, Object>> getLeaderboard(int limit) {
        log.info("🏆 Fetching top {} users for leaderboard", limit);
        List<User> topUsers = userRepository.findTopUsers(PageRequest.of(0, limit));

        return topUsers.stream().map(u -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("username", u.getUsername());
            entry.put("points", u.getPoints());
            entry.put("badges", Arrays.stream(
                            Optional.ofNullable(u.getBadges()).orElse("")
                                    .split(","))
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList()));
            return entry;
        }).collect(Collectors.toList());
    }

    // ==============================
    // 🎮 Gamification Logic (unchanged)
    // ==============================

    @Transactional
    public void updateGamification(User user, int score) {
        int pointsToAdd;
        if (score >= 80) pointsToAdd = 50;
        else if (score >= 50) pointsToAdd = 25;
        else pointsToAdd = 10;

        int newTotal = user.getPoints() + pointsToAdd;
        user.setPoints(newTotal);

        StringBuilder badges = new StringBuilder(
                Optional.ofNullable(user.getBadges()).orElse(""));

        if (newTotal >= 1000 && !badges.toString().contains("Gold")) {
            badges.append(badges.length() > 0 ? "," : "").append("Gold");
        } else if (newTotal >= 500 && !badges.toString().contains("Silver")) {
            badges.append(badges.length() > 0 ? "," : "").append("Silver");
        } else if (newTotal >= 200 && !badges.toString().contains("Bronze")) {
            badges.append(badges.length() > 0 ? "," : "").append("Bronze");
        }

        user.setBadges(badges.toString());
        userRepository.save(user);

        log.info("🎮 Updated gamification for user {}: +{} points → total {} | Badges: {}",
                user.getId(), pointsToAdd, newTotal, user.getBadges());
    }

    /**
     * ✅ Classroom Analytics (stub)
     */
    public Map<String, Object> getClassroomAnalytics(Long classId) {
        log.info("📘 Classroom analytics requested for class {}", classId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Classroom analytics not implemented yet");
        return response;
    }

    @Transactional
    public void recalculateAllLeaderboard() {
        log.info("🔄 Recalculating leaderboard (future batch logic placeholder)");
    }
}
