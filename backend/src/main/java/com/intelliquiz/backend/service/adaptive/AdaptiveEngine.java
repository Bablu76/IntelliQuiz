package com.intelliquiz.backend.service.adaptive;

import com.intelliquiz.backend.model.QuizAttempt;
import com.intelliquiz.backend.repository.QuizAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 🎯 AdaptiveEngine
 * Learns from user's historical quiz performance and intelligently suggests
 * the next difficulty level for quizzes.
 *
 * Logic:
 *  - Uses the user's average score across the last N attempts (topic-based)
 *  - Considers both performance trend and last attempted difficulty
 *  - Default difficulty is "medium" for new learners
 */
@Service
public class AdaptiveEngine {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveEngine.class);

    private final QuizAttemptRepository quizAttemptRepository;

    @Autowired
    public AdaptiveEngine(QuizAttemptRepository quizAttemptRepository) {
        this.quizAttemptRepository = quizAttemptRepository;
    }

    /**
     * Determines next quiz difficulty for the given user and topic.
     * @param userId user ID
     * @param topic quiz topic
     * @return suggested difficulty level ("easy", "medium", or "hard")
     */
    public String suggestNextDifficulty(Long userId, String topic) {
        try {
            List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdAndTopic(userId, topic);

            if (attempts == null || attempts.isEmpty()) {
                log.info("🧩 No past attempts for user {} topic '{}'. Returning 'medium' as default.", userId, topic);
                return "medium";
            }

            // Compute average score across attempts
            double avgScore = attempts.stream()
                    .mapToInt(QuizAttempt::getScore)
                    .average()
                    .orElse(0);

            // Extract last attempt difficulty
            QuizAttempt lastAttempt = attempts.get(attempts.size() - 1);
            String lastDifficulty = lastAttempt.getDifficultyLevel();

            // Compute improvement trend (compare last vs avg)
            double lastScore = lastAttempt.getScore();
            double trendDelta = lastScore - avgScore;

            String nextDifficulty = computeNextDifficulty(avgScore, trendDelta, lastDifficulty);

            log.info("🧠 AdaptiveEngine Decision → user={} | topic='{}' | avgScore={} | lastScore={} | trendΔ={} | last='{}' → next='{}'",
                    userId, topic, avgScore, lastScore, trendDelta, lastDifficulty, nextDifficulty);

            return nextDifficulty;

        } catch (Exception e) {
            log.error("⚠️ AdaptiveEngine failed for user {} topic '{}': {}", userId, topic, e.getMessage());
            return "medium";
        }
    }

    /**
     * Internal logic for computing next difficulty based on current performance metrics.
     */
    private String computeNextDifficulty(double avgScore, double trendDelta, String lastDifficulty) {
        // 🔹 Adaptive rules:
        // - Average >= 85 → harder level
        // - Average <= 50 → easier level
        // - Moderate → stay same
        // - Trend improvements accelerate progression

        if (avgScore >= 85 || (trendDelta > 10 && !"hard".equalsIgnoreCase(lastDifficulty))) {
            return "hard";
        } else if (avgScore <= 50 || (trendDelta < -10 && !"easy".equalsIgnoreCase(lastDifficulty))) {
            return "easy";
        } else {
            return "medium";
        }
    }
}
