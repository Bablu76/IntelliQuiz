package com.intelliquiz.backend.controller;

import com.intelliquiz.backend.model.QuizAttempt;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.repository.QuizAttemptRepository;
import com.intelliquiz.backend.repository.UserRepository;
import com.intelliquiz.backend.service.AnalyticsService;
import com.intelliquiz.backend.service.ContentQuizService;
import com.intelliquiz.backend.service.PdfService;
import com.intelliquiz.backend.service.adaptive.AdaptiveEngine;
import com.intelliquiz.backend.service.llm.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.Serializable;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🎯 QuizController
 * Handles quiz generation (LLM + adaptive), submission, analytics, and fallback mock tests.
 */
@RestController
@RequestMapping("/quiz")
@CrossOrigin(origins = "http://localhost:5173")
public class QuizController {

    private static final Logger log = LoggerFactory.getLogger(QuizController.class);

    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AnalyticsService analyticsService;
    @Autowired private ContentQuizService contentQuizService;
    @Autowired private LLMService llmService;
    @Autowired private AdaptiveEngine adaptiveEngine;
    @Autowired private PdfService pdfService;

    // ------------------ 1️⃣ Dummy Endpoint ------------------
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

    // ------------------ 2️⃣ LLM + Adaptive Quiz Generator ------------------
    @PostMapping("/generate/ai")
    public ResponseEntity<?> generateAIQuiz(@RequestBody Map<String, Object> payload, Principal principal) {
        String topic = (String) payload.getOrDefault("topic", "General Knowledge");
        String pdfContext = (String) payload.get("pdfContext");
        int questionCount = (int) payload.getOrDefault("questionCount", 5);

        if (pdfContext == null || pdfContext.trim().isEmpty()) {
            log.warn("⚠️ Missing pdfContext for topic '{}'", topic);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "pdfContext is required to generate AI-based quiz"));
        }

        // 🔹 Truncate long contexts for token limit safety
        if (pdfContext.length() > 8000) {
            pdfContext = pdfContext.substring(0, 8000);
            log.warn("⚠️ Truncated pdfContext to 8000 characters.");
        }

        // Identify user
        User user = null;
        if (principal != null) {
            user = userRepository.findByUsername(principal.getName()).orElse(null);
        }

        // 🔹 Compute adaptive difficulty
        String adaptiveDifficulty = (user != null)
                ? adaptiveEngine.suggestNextDifficulty(user.getId(), topic)
                : "medium";

        log.info("🤖 /quiz/generate/ai | topic='{}' | adaptiveDifficulty='{}'", topic, adaptiveDifficulty);

        try {
            // 🔹 Generate questions using LLM
            List<Map<String, Object>> questions = llmService.generateQuestions(
                    topic, adaptiveDifficulty, pdfContext, questionCount
            );

            if (questions == null || questions.isEmpty()) {
                log.warn("⚠️ LLM returned empty results. Falling back to mock questions.");
                questions = generateMockQuestions(topic, adaptiveDifficulty);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("topic", topic);
            response.put("difficultyLevel", adaptiveDifficulty);
            response.put("questions", questions);
            response.put("count", questions.size());
            response.put("message", "✅ AI quiz generated successfully.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error generating AI quiz for topic '{}': {}", topic, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Quiz generation failed", "details", e.getMessage()));
        }
    }

    // ------------------ 3️⃣ Classic Quiz Generator (non-LLM) ------------------
    @GetMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateQuiz(
            @RequestParam(defaultValue = "General") String topic,
            @RequestParam(defaultValue = "medium") String difficulty) {

        log.info("🎯 /quiz/generate called | topic='{}' difficulty='{}'", topic, difficulty);

        try {
            List<Map<String, Object>> questions = contentQuizService.generateQuestionsFromTopic(topic, difficulty);

            if (questions == null || questions.isEmpty()) {
                log.warn("⚠️ No questions found for topic '{}'", topic);
                return ResponseEntity.ok(Map.of(
                        "topic", topic,
                        "difficultyLevel", difficulty,
                        "message", "No questions found for this topic.",
                        "questions", Collections.emptyList()
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "topic", topic,
                    "difficultyLevel", difficulty,
                    "questions", questions
            ));

        } catch (Exception e) {
            log.error("❌ Error generating static quiz: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to generate quiz"));
        }
    }

    // ------------------ 4️⃣ Fetch User Quiz History ------------------
    @GetMapping("/attempts/{userId}")
    public ResponseEntity<?> getUserAttempts(@PathVariable Long userId) {
        try {
            List<QuizAttempt> attempts = quizAttemptRepository.findByUserId(userId);
            if (attempts == null || attempts.isEmpty()) return ResponseEntity.ok(Collections.emptyList());

            List<Map<String, ? extends Serializable>> dto = attempts.stream().map(a -> Map.of(
                    "id", a.getId(),
                    "topic", a.getTopic(),
                    "score", a.getScore(),
                    "difficultyLevel", a.getDifficultyLevel(),
                    "date", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "N/A"
            )).collect(Collectors.toList());

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            log.error("❌ Error fetching quiz attempts for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch quiz attempts"));
        }
    }

    // ------------------ 5️⃣ Submit Quiz & Update Analytics ------------------
    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestBody Map<String, Object> request, Principal principal) {
        try {
            // 🔹 Identify user
            Integer incomingUserId = (Integer) request.get("userId");
            String username = principal != null ? principal.getName() : null;

            User currentUser = (username != null)
                    ? userRepository.findByUsername(username).orElse(null)
                    : userRepository.findById(incomingUserId.longValue()).orElse(null);

            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "User not found or not authenticated."));
            }

            Long userId = currentUser.getId();

            // 🔹 Extract quiz info
            List<Map<String, Object>> answers = (List<Map<String, Object>>) request.get("answers");
            String topic = (String) request.getOrDefault("topic", "General");
            String difficultyLevel = (String) request.getOrDefault("difficulty", "medium");
            int timeTaken = (Integer) request.getOrDefault("timeTaken", 0);

            if (answers == null || answers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Answers list cannot be empty"));
            }

            // 🔹 Compute score
            long correctCount = answers.stream()
                    .filter(ans -> ans.get("isCorrect") instanceof Boolean && (Boolean) ans.get("isCorrect"))
                    .count();

            int total = answers.size();
            int percentage = (int) Math.round((correctCount * 100.0) / total);
            String nextLevel = determineNextLevel((int) correctCount, total, difficultyLevel);

            // 🔹 Save Attempt
            QuizAttempt attempt = new QuizAttempt(currentUser, topic, difficultyLevel, percentage, timeTaken);
            quizAttemptRepository.save(attempt);

            log.info("📊 Saved quiz attempt for user {} → {}% [{} → next: {}]", userId, percentage, difficultyLevel, nextLevel);

            // 🔹 Update Gamification Analytics
            try {
                analyticsService.updateGamification(currentUser, percentage);
            } catch (Exception e) {
                log.warn("⚠️ Gamification update failed for user {}: {}", userId, e.getMessage());
            }

            // 🔹 Response
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "scorePercentage", percentage,
                    "correctAnswers", correctCount,
                    "totalQuestions", total,
                    "nextLevel", nextLevel,
                    "difficultyUsed", difficultyLevel,
                    "topic", topic
            ));

        } catch (Exception e) {
            log.error("❌ Error submitting quiz: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error processing quiz submission"));
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
        Map<String, List<Map<String, Object>>> bank = new HashMap<>();

        bank.put("AI-medium", List.of(
                createQuestion("What is machine learning?",
                        List.of("Hardware", "Learning from data", "Manual coding", "Cloud computing"),
                        "Learning from data", "medium"),
                createQuestion("Which algorithm is used for classification?",
                        List.of("Decision Tree", "Bubble Sort", "Binary Search", "Merge Sort"),
                        "Decision Tree", "medium")
        ));

        bank.put("General-medium", List.of(
                createQuestion("Who wrote India's national anthem?",
                        List.of("Tagore", "Gandhi", "Nehru", "Bose"),
                        "Tagore", "medium"),
                createQuestion("Which planet is the Red Planet?",
                        List.of("Earth", "Mars", "Jupiter", "Venus"),
                        "Mars", "medium")
        ));

        String key = topic + "-" + difficulty;
        List<Map<String, Object>> topicQs = bank.getOrDefault(key, new ArrayList<>());

        if (topicQs.isEmpty()) {
            for (int i = 0; i < 5; i++) {
                questions.add(createQuestion(
                        "Sample question about " + topic + "?",
                        List.of("Option A", "Option B", "Option C", "Option D"),
                        "Option A", difficulty
                ));
            }
        } else questions.addAll(topicQs);

        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).put("questionId", i + 1);
        }

        return questions;
    }

    private Map<String, Object> createQuestion(String question, List<String> options, String answer, String difficulty) {
        Map<String, Object> q = new HashMap<>();
        q.put("question", question);
        q.put("options", options);
        q.put("answer", answer);
        q.put("difficultyLevel", difficulty);
        return q;
    }

    @PostMapping("/test/pdf")
    @CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> testPdfExtract(@RequestParam("file") MultipartFile file) {
        try {
            String extractedText = pdfService.extractText(file);
            Map<String, Object> result = Map.of(
                    "fileName", Objects.requireNonNull(file.getOriginalFilename()),
                    "sizeKB", file.getSize() / 1024,
                    "sampleText", extractedText.length() > 500
                            ? extractedText.substring(0, 500) + "..."
                            : extractedText
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ PDF extraction failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "PDF extraction failed",
                            "details", e.getMessage()
                    ));
        }
    }
    @PostMapping("/test/llm")
    public ResponseEntity<?> testLLMWithPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "AI and Machine Learning") String topic,
            @RequestParam(defaultValue = "medium") String difficulty,
            @RequestParam(value = "questionCount", defaultValue = "10") int questionCount
    ) {
        Instant start = Instant.now();

        try {
            // ✅ 1️⃣ Extract PDF text safely
            String pdfText = pdfService.extractText(file);
            if (pdfText == null || pdfText.isBlank()) {
                throw new IllegalArgumentException("PDF text extraction returned empty content.");
            }

            // ✅ 2️⃣ Cap or auto-scale question count for safety
            int safeCount = Math.min(Math.max(1, questionCount), 100); // limit 1–100

            // ✅ 3️⃣ (Optional) Smart default if user didn’t specify
            if (questionCount <= 0) {
                int len = pdfText.length();
                if (len < 50000) safeCount = 10;
                else if (len < 150000) safeCount = 25;
                else if (len < 300000) safeCount = 40;
                else safeCount = 60;
            }

            // ✅ 4️⃣ Pass the full text; chunking is handled inside LLMService
            List<Map<String, Object>> questions =
                    llmService.generateQuestions(topic, difficulty, pdfText, safeCount);

            // ✅ 5️⃣ Construct a rich response object
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("topic", topic);
            response.put("difficulty", difficulty);
            response.put("questionsRequested", safeCount);
            response.put("questionsGenerated", questions.size());
            response.put("pdfCharacters", pdfText.length());
            response.put("generationTimeMs", Instant.now().toEpochMilli() - start.toEpochMilli());
            response.put("questions", questions);

            log.info("✅ Quiz generated: {} questions ({} chars processed, {} ms)",
                    questions.size(), pdfText.length(),
                    Instant.now().toEpochMilli() - start.toEpochMilli());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException iae) {
            log.error("⚠️ Invalid request: {}", iae.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid input",
                    "details", iae.getMessage()
            ));

        } catch (Exception e) {
            log.error("❌ LLM generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "LLM quiz generation failed",
                            "details", e.getMessage()
                    ));
        }
    }

}
