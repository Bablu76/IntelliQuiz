package com.intelliquiz.backend.security.services;

import com.intelliquiz.backend.model.QuizAttempt;
import com.intelliquiz.backend.model.User;
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

    public List<QuizAttempt> getAttemptsByUser(User user) {
        return quizAttemptRepository.findByUser(user);
    }

    public double calculateAverageScore(User user) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByUser(user);
        return attempts.stream()
                .mapToInt(QuizAttempt::getScore)
                .average()
                .orElse(0.0);
    }


    /**
     * Returns analytics metrics for a specific student.
     */
    public Map<String, Object> getStudentAnalytics(Long userId) {
        log.info("📊 Fetching analytics for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Double avgScore = quizAttemptRepository.findAverageScore(userId);
        avgScore = (avgScore == null) ? 0.0 : Math.round(avgScore * 100.0) / 100.0;

        List<Integer> recentScores = quizAttemptRepository.findLastFiveScores(userId);
        Collections.reverse(recentScores); // chronological (oldest → newest)

        double accuracy = avgScore; // no totalQuestions field yet
        int points = user.getPoints();
        List<String> badges = Arrays.stream(
                        Optional.ofNullable(user.getBadges()).orElse("")
                                .split(","))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("averageScore", avgScore);
        response.put("accuracy", accuracy);
        response.put("trend", recentScores);
        response.put("points", points);
        response.put("badges", badges);

        log.debug("✅ Analytics response built for user {}: {}", userId, response);
        return response;
    }

    /**
     * Generates leaderboard with top N users by points.
     */
    public List<Map<String, Object>> getLeaderboard(int limit) {
        log.info("🏆 Fetching top {} users for leaderboard", limit);
        List<User> topUsers = userRepository.findTopUsers(PageRequest.of(0, limit));

        List<Map<String, Object>> leaderboard = topUsers.stream().map(u -> {
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

        log.debug("🏅 Leaderboard size: {}", leaderboard.size());
        return leaderboard;
    }

    // ==============================
    // 🎮 Gamification Logic
    // ==============================

    /**
     * Update user points & badges after quiz submission.
     */
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

        // badge thresholds
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


    public Map<String, Object> getClassroomAnalytics(Long classId) {
        log.info("📘 Classroom analytics requested for class {}", classId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Classroom analytics not implemented yet");
        return response;
    }



    @Transactional
    public void recalculateAllLeaderboard() {
        log.info("🔄 Recalculating leaderboard...");
        // placeholder for batch logic if needed later
    }
}
