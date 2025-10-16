package com.intelliquiz.backend.controller;

import com.intelliquiz.backend.model.QuizAttempt;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.repository.QuizAttemptRepository;
import com.intelliquiz.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🎯 QuizController
 * Handles quiz generation, submission, and test endpoints.
 * Includes structured SLF4J logging for debugging and analytics.
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

        log.debug("🧩 Generated {} mock questions for topic '{}' with difficulty '{}'",
                questions.size(), topic, difficulty);

        return ResponseEntity.ok(response);
    }

    // ------------------ 3️⃣ Submit Quiz ------------------
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitQuiz(
            @RequestBody Map<String, Object> request,
            Principal principal) {

        // 1️⃣ Identify user (supports both JWT principal and explicit userId in request)
        Integer incomingUserId = (Integer) request.get("userId");
        String username = principal != null ? principal.getName() : null;

        User resolvedUser;

        if (username != null) {
            resolvedUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
        } else if (incomingUserId != null) {
            resolvedUser = userRepository.findById(incomingUserId.longValue())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + incomingUserId));
        } else {
            log.warn("⚠️ No user identified in quiz submission");
            return ResponseEntity.badRequest().body(Map.of("message", "Missing user identification"));
        }

        // ✅ Make them final (for lambda/log use)
        final int userId = resolvedUser.getId().intValue();
        final User currentUser = resolvedUser;

        // 2️⃣ Extract quiz info
        List<Map<String, Object>> answers = (List<Map<String, Object>>) request.get("answers");
        String topic = (String) request.getOrDefault("topic", "General");
        String difficulty = (String) request.getOrDefault("difficulty", "medium");

        if (answers == null || answers.isEmpty()) {
            log.warn("⚠️ User {} submitted null or empty answers list", userId);
            return ResponseEntity.badRequest().body(Map.of("message", "Answers list cannot be empty"));
        }

        // 3️⃣ Compute score
        int correctCount = 0;
        for (Map<String, Object> answer : answers) {
            if (answer == null || !answer.containsKey("isCorrect")) continue;
            Boolean isCorrect = (Boolean) answer.get("isCorrect");
            if (Boolean.TRUE.equals(isCorrect)) correctCount++;
        }

        // 4️⃣ Adaptive next level
        String nextLevel;
        if (correctCount >= 4) nextLevel = "hard";
        else if (correctCount <= 2) nextLevel = "easy";
        else nextLevel = "medium";

        log.info("🧠 User {} submitted quiz — Score: {}/{} → Next Level: {}",
                userId, correctCount, answers.size(), nextLevel);

        // 5️⃣ Save quiz attempt in DB
        try {
            QuizAttempt attempt = new QuizAttempt(
                    currentUser, topic, difficulty, correctCount
            );
            quizAttemptRepository.save(attempt);
            log.debug("📊 Saved quiz attempt for user {} → Attempt ID: {}", userId, attempt.getId());
        } catch (Exception e) {
            log.error("❌ Failed to save quiz attempt for user {}: {}", userId, e.getMessage(), e);
        }

        // 6️⃣ Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("score", correctCount);
        response.put("totalQuestions", answers.size());
        response.put("nextLevel", nextLevel);

        log.debug("📊 Response prepared for user {}: {}", userId, response);
        return ResponseEntity.ok(response);
    }

    // ------------------ Helper Methods ------------------

    private List<Map<String, Object>> generateMockQuestions(String topic, String difficulty) {
        List<Map<String, Object>> questions = new ArrayList<>();

        Map<String, List<Map<String, Object>>> questionBank = new HashMap<>();

        // ✅ Add topic sets
        questionBank.put("AI-medium", List.of(
                createQuestion("What is machine learning?",
                        List.of("Hardware component", "Computers learning patterns from data", "Manual rule setting", "Cloud storage"),
                        "Computers learning patterns from data", "medium"),
                createQuestion("Which algorithm is used for classification?",
                        List.of("Decision Tree", "Bubble Sort", "Binary Search", "Merge Sort"),
                        "Decision Tree", "medium")
        ));

        questionBank.put("General Knowledge-medium", List.of(
                createQuestion("Who wrote the national anthem of India?",
                        List.of("Rabindranath Tagore", "Mahatma Gandhi", "Jawaharlal Nehru", "Subhash Chandra Bose"),
                        "Rabindranath Tagore", "medium"),
                createQuestion("Which planet is known as the Red Planet?",
                        List.of("Earth", "Mars", "Jupiter", "Venus"),
                        "Mars", "medium"),
                createQuestion("How many continents are there on Earth?",
                        List.of("5", "6", "7", "8"),
                        "7", "medium"),
                createQuestion("What is H2O commonly known as?",
                        List.of("Oxygen", "Hydrogen", "Water", "Salt"),
                        "Water", "medium"),
                createQuestion("Which gas do plants absorb during photosynthesis?",
                        List.of("Carbon Dioxide", "Oxygen", "Nitrogen", "Helium"),
                        "Carbon Dioxide", "medium")
        ));

        String key = topic + "-" + difficulty;
        List<Map<String, Object>> topicQuestions = questionBank.getOrDefault(key, new ArrayList<>());

        if (topicQuestions.isEmpty()) {
            log.warn("⚠️ No predefined questions found for topic '{}' and difficulty '{}'. Using fallback set.", topic, difficulty);
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

        log.info("📚 Prepared {} total questions for topic '{}' [{}]", questions.size(), topic, difficulty);
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
