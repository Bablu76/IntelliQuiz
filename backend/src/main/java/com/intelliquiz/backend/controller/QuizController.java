package com.intelliquiz.backend.controller;

import com.intelliquiz.backend.model.QuizAttempt;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.repository.QuizAttemptRepository;
import com.intelliquiz.backend.repository.UserRepository;
import com.intelliquiz.backend.security.services.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🎯 QuizController
 * Handles quiz generation, submission, and mock testing.
 * Includes adaptive logic and structured SLF4J logging.
 */
@RestController
@RequestMapping("/quiz")
@CrossOrigin(origins = "http://localhost:5173")
public class QuizController {

    private static final Logger log = LoggerFactory.getLogger(QuizController.class);

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnalyticsService analyticsService;

    // ------------------ 1️⃣ Dummy Test Endpoint ------------------
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> getDummyQuiz() {
        log.debug("📘 GET /quiz/test called — returning static sample quiz");

        Map<String, Object> quiz = new HashMap<>();
        quiz.put("id", 1);
        quiz.put("title", "Sample Quiz");
        quiz.put("questions", List.of(
                Map.of("questionId", 1, "question", "What is 2+2?",
                        "options", List.of("3", "4", "5"), "answer", "4"),
                Map.of("questionId", 2, "question", "Capital of France?",
                        "options", List.of("Berlin", "Paris", "Rome"), "answer", "Paris")
        ));

        log.info("✅ Served dummy quiz with {} questions", ((List<?>) quiz.get("questions")).size());
        return ResponseEntity.ok(quiz);
    }

    // ------------------ 2️⃣ Generate Quiz ------------------
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateQuiz(@RequestBody Map<String, String> request) {
        String topic = request.getOrDefault("topic", "AI");
        String difficulty = request.getOrDefault("difficulty", "medium");

        log.info("🎯 Generating quiz | Topic: {} | Difficulty: {}", topic, difficulty);

        List<Map<String, Object>> questions = generateMockQuestions(topic, difficulty);

        Map<String, Object> response = new HashMap<>();
        response.put("topic", topic);
        response.put("difficulty", difficulty);
        response.put("questions", questions);

        log.debug("🧩 Generated {} mock questions for topic '{}' [{}]",
                questions.size(), topic, difficulty);

        return ResponseEntity.ok(response);
    }

    // ------------------ 3️⃣ Submit Quiz ------------------
    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(
            @RequestBody Map<String, Object> request,
            Principal principal) {

        try {
            // 1️⃣ Identify the user
            Integer incomingUserId = (Integer) request.get("userId");
            String username = (principal != null) ? principal.getName() : null;

            User currentUser = null;

            if (username != null) {
                currentUser = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found: " + username));
            } else if (incomingUserId != null) {
                currentUser = userRepository.findById(incomingUserId.longValue())
                        .orElseThrow(() -> new RuntimeException("User not found with ID: " + incomingUserId));
            }

            if (currentUser == null) {
                log.warn("⚠️ No user identified in quiz submission");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Missing or invalid user identification"));
            }

            Long userId = currentUser.getId();

            // 2️⃣ Extract quiz info
            List<Map<String, Object>> answers = (List<Map<String, Object>>) request.get("answers");
            String topic = (String) request.getOrDefault("topic", "General");
            String difficulty = (String) request.getOrDefault("difficulty", "medium");
            int timeTaken = request.containsKey("timeTaken") ? (Integer) request.get("timeTaken") : 0;

            if (answers == null || answers.isEmpty()) {
                log.warn("⚠️ User {} submitted null or empty answers list", userId);
                return ResponseEntity.badRequest().body(Map.of("message", "Answers list cannot be empty"));
            }

            // 3️⃣ Compute score (convert to percentage)
            int correctCount = 0;
            for (Map<String, Object> answer : answers) {
                if (answer == null || !answer.containsKey("isCorrect")) continue;
                Boolean isCorrect = (Boolean) answer.get("isCorrect");
                if (Boolean.TRUE.equals(isCorrect)) correctCount++;
            }

            int totalQuestions = answers.size();
            int percentageScore = (int) Math.round((correctCount * 100.0) / totalQuestions);

            // 4️⃣ Adaptive next level (based on percentage)
            String nextLevel = determineNextLevel(percentageScore, totalQuestions, difficulty);

            log.info("🧠 User {} submitted quiz — Score: {}% ({} of {}) → Next Level: {}",
                    userId, percentageScore, correctCount, totalQuestions, nextLevel);

            // 5️⃣ Save quiz attempt in DB (percentage-based)
            QuizAttempt attempt = new QuizAttempt(
                    currentUser, topic, difficulty, percentageScore, timeTaken
            );
            quizAttemptRepository.save(attempt);
            log.debug("📊 Saved quiz attempt for user {} → Attempt ID: {}", userId, attempt.getId());

            // 5️⃣.5 🎮 Update Gamification (points & badges)
            try {
                analyticsService.updateGamification(currentUser, percentageScore);
                log.info("🏅 Gamification updated successfully for user {} | Score: {}%", userId, percentageScore);
            } catch (Exception e) {
                log.error("⚠️ Error updating gamification for user {}: {}", userId, e.getMessage());
                // Do not break the quiz submission if gamification fails
            }

            // 6️⃣ Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("scorePercentage", percentageScore);
            response.put("correctAnswers", correctCount);
            response.put("totalQuestions", totalQuestions);
            response.put("nextLevel", nextLevel);
            response.put("difficultyUsed", difficulty);
            response.put("topic", topic);

            log.debug("📊 Response prepared for user {}: {}", userId, response);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Quiz submission failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error while processing quiz submission"));
        }
    }

    // ------------------ Helper Methods ------------------

    private String determineNextLevel(int correct, int total, String current) {
        double accuracy = (correct * 100.0) / total;
        if (accuracy >= 80 && !"hard".equalsIgnoreCase(current)) return "hard";
        if (accuracy <= 50 && !"easy".equalsIgnoreCase(current)) return "easy";
        return "medium";
    }

    private List<Map<String, Object>> generateMockQuestions(String topic, String difficulty) {
        List<Map<String, Object>> questions = new ArrayList<>();

        Map<String, List<Map<String, Object>>> questionBank = new HashMap<>();

        // ✅ Example topic sets
        questionBank.put("AI-medium", List.of(
                createQuestion("What is machine learning?",
                        List.of("Hardware component", "Computers learning patterns from data",
                                "Manual rule setting", "Cloud storage"),
                        "Computers learning patterns from data", "medium"),
                createQuestion("Which algorithm is used for classification?",
                        List.of("Decision Tree", "Bubble Sort", "Binary Search", "Merge Sort"),
                        "Decision Tree", "medium")
        ));

        questionBank.put("General-medium", List.of(
                createQuestion("Who wrote the national anthem of India?",
                        List.of("Rabindranath Tagore", "Mahatma Gandhi", "Jawaharlal Nehru", "Subhash Chandra Bose"),
                        "Rabindranath Tagore", "medium"),
                createQuestion("Which planet is known as the Red Planet?",
                        List.of("Earth", "Mars", "Jupiter", "Venus"),
                        "Mars", "medium")
        ));

        String key = topic + "-" + difficulty;
        List<Map<String, Object>> topicQuestions = questionBank.getOrDefault(key, new ArrayList<>());

        if (topicQuestions.isEmpty()) {
            log.warn("⚠️ No predefined questions for topic '{}' [{}]. Using fallback set.", topic, difficulty);
            for (int i = 0; i < 5; i++) {
                questions.add(createQuestion(
                        "Sample question about " + topic + "?",
                        List.of("Option A", "Option B", "Option C", "Option D"),
                        "Option A",
                        difficulty
                ));
            }
        } else {
            questions.addAll(topicQuestions);
        }

        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).put("questionId", i + 1);
        }

        log.info("📚 Prepared {} questions for topic '{}' [{}]", questions.size(), topic, difficulty);
        return questions;
    }

    private Map<String, Object> createQuestion(String question, List<String> options, String answer, String difficulty) {
        Map<String, Object> q = new HashMap<>();
        q.put("question", question);
        q.put("options", options);
        q.put("answer", answer);
        q.put("difficulty", difficulty);
        return q;
    }
}
