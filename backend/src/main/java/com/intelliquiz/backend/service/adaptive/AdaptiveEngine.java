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
 * Learns from user’s past quiz performance and determines next difficulty level.
 * Logic:
 *  - Evaluates average score + trend for each topic
 *  - Adjusts difficulty progressively after each quiz submission
 *  - Never changes difficulty before the first attempt
 */
@Service
public class AdaptiveEngine {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveEngine.class);

    private final QuizAttemptRepository quizAttemptRepository;

    @Autowired
    public AdaptiveEngine(QuizAttemptRepository quizAttemptRepository) {
        this.quizAttemptRepository = quizAttemptRepository;
    }

    // -------------------------------------------------------------------------
    // 🧠 1️⃣ Suggest Next Difficulty (used only if user already attempted)
    // -------------------------------------------------------------------------
    public String suggestNextDifficulty(Long userId, String topic) {
        try {
            List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdAndTopic(userId, topic);

            if (attempts == null || attempts.isEmpty()) {
                log.info("🧩 No past attempts for user={} topic='{}'. Defaulting to 'medium'.", userId, topic);
                return "medium";
            }

            double avgScore = attempts.stream().mapToInt(QuizAttempt::getScore).average().orElse(0);
            QuizAttempt lastAttempt = attempts.get(attempts.size() - 1);
            double lastScore = lastAttempt.getScore();
            String lastDiff = lastAttempt.getDifficultyLevel();

            double trend = lastScore - avgScore;
            String next = computeNextDifficulty(avgScore, trend, lastDiff);

            log.info("""
                    🧠 Adaptive Decision
                    ├─ User: {}
                    ├─ Topic: '{}'
                    ├─ Avg Score: {}
                    ├─ Last Score: {}
                    ├─ Trend Δ: {}
                    ├─ Last Difficulty: {}
                    └─ Next Difficulty: {}
                    """, userId, topic, avgScore, lastScore, trend, lastDiff, next);

            return next;
        } catch (Exception e) {
            log.error("⚠️ AdaptiveEngine failure for user={} topic='{}': {}", userId, topic, e.getMessage());
            return "medium";
        }
    }

    // -------------------------------------------------------------------------
    // 🧩 2️⃣ Compute Difficulty Logic
    // -------------------------------------------------------------------------
    private String computeNextDifficulty(double avgScore, double trendDelta, String lastDifficulty) {
        if (avgScore >= 85 || (trendDelta > 10 && !"hard".equalsIgnoreCase(lastDifficulty))) {
            return "hard";
        } else if (avgScore <= 50 || (trendDelta < -10 && !"easy".equalsIgnoreCase(lastDifficulty))) {
            return "easy";
        } else {
            return "medium";
        }
    }

    // -------------------------------------------------------------------------
    // 🧩 3️⃣ Get Last Attempted Difficulty (for same topic)
    // -------------------------------------------------------------------------
    public String getLastDifficulty(Long userId, String topic) {
        try {
            List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdAndTopic(userId, topic);
            if (attempts == null || attempts.isEmpty()) {
                log.info("🆕 New topic '{}' for user={} → Starting at 'medium'.", topic, userId);
                return "medium";
            }

            QuizAttempt last = attempts.get(attempts.size() - 1);
            String lastDiff = last.getDifficultyLevel() != null ? last.getDifficultyLevel() : "medium";
            log.info("📊 Retrieved last difficulty='{}' for user={} topic='{}'", lastDiff, userId, topic);
            return lastDiff;
        } catch (Exception e) {
            log.warn("⚠️ getLastDifficulty failed for user={} topic='{}': {}", userId, topic, e.getMessage());
            return "medium";
        }
    }

    // -------------------------------------------------------------------------
    // 🧩 4️⃣ Update After Submission (called once user completes a quiz)
    // -------------------------------------------------------------------------
    public String updateAfterSubmission(Long userId, String topic, int score, String prevDifficulty) {
        try {
            String next = "medium";

            if (score >= 85 && !"hard".equalsIgnoreCase(prevDifficulty)) next = "hard";
            else if (score <= 50 && !"easy".equalsIgnoreCase(prevDifficulty)) next = "easy";
            else next = prevDifficulty; // maintain same level

            log.info("""
                    📈 Adaptive Update (After Submission)
                    ├─ User: {}
                    ├─ Topic: '{}'
                    ├─ Score: {}
                    ├─ Previous Difficulty: {}
                    └─ Next Difficulty: {}
                    """, userId, topic, score, prevDifficulty, next);

            return next;
        } catch (Exception e) {
            log.error("❌ Adaptive update failed for user={} topic='{}': {}", userId, topic, e.getMessage());
            return "medium";
        }
    }
}
