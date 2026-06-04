package com.intelliquiz.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelliquiz.backend.model.GeneratedQuiz;
import com.intelliquiz.backend.model.QuizAttempt;
import com.intelliquiz.backend.model.Resource;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.repository.GeneratedQuizRepository;
import com.intelliquiz.backend.repository.QuizAttemptRepository;
import com.intelliquiz.backend.repository.UserRepository;
import com.intelliquiz.backend.service.*;
import com.intelliquiz.backend.service.adaptive.AdaptiveEngine;
import com.intelliquiz.backend.service.llm.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/quiz")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class QuizController {

    private static final Logger log = LoggerFactory.getLogger(QuizController.class);

    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GeneratedQuizRepository generatedQuizRepository;
    @Autowired private AnalyticsService analyticsService;
    @Autowired private ResourceService resourceService;
    @Autowired private PdfService pdfService;
    @Autowired private LLMService llmService;
    @Autowired private AdaptiveEngine adaptiveEngine;

    private final ObjectMapper mapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // 🧠 1️⃣ Smart AI Quiz Generator — Supports file or stored resource
    // -------------------------------------------------------------------------
    @PostMapping(value = "/generate/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> generateAIQuiz(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) Long resourceId,
            @RequestParam String topic,
            @RequestParam(defaultValue = "medium") String difficulty,
            @RequestParam(defaultValue = "10") int questionCount,
            Principal principal) {

        Instant start = Instant.now();
        String context = "";
        String adaptiveDiff = difficulty;
        User user = null;

        try {
            if (principal != null) {
                user = userRepository.findByUsername(principal.getName()).orElse(null);
            }

            // 🧩 1. Determine text source
            if (file != null && !file.isEmpty()) {
                context = pdfService.extractText(file);
                log.info("📄 Extracted {} chars from uploaded PDF '{}'", context.length(), file.getOriginalFilename());
            } else if (resourceId != null) {
                Resource resource = resourceService.getResourceById(resourceId);
                context = resource.getExtractedText();
                log.info("📘 Loaded {} chars from saved resource '{}'", context.length(), resource.getFileName());
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Either file or resourceId is required."));
            }

            if (context.isBlank()) {
                throw new IllegalArgumentException("No readable content found in source.");
            }

            // 🧠 2. Handle adaptive difficulty
            boolean hasPastAttempt = false;
            if (user != null) {
                var pastAttempts = quizAttemptRepository.findByUserIdAndTopic(user.getId(), topic);
                hasPastAttempt = pastAttempts != null && !pastAttempts.isEmpty();
            }

            if (user != null && hasPastAttempt) {
                adaptiveDiff = adaptiveEngine.getLastDifficulty(user.getId(), topic);
                log.info("🎯 Existing topic detected → using last difficulty '{}' for '{}'", adaptiveDiff, topic);
            } else {
                adaptiveDiff = difficulty;
                log.info("🆕 New topic → starting at user-selected difficulty '{}'", difficulty);
            }

            // 🧩 3. Cache check — skip the (paid) LLM call if we already
            //        generated a quiz for an identical request.
            String contentHash = computeContentHash(context, topic, adaptiveDiff, questionCount);
            var cachedOpt = generatedQuizRepository.findFirstByContentHashOrderByGeneratedAtDesc(contentHash);
            if (cachedOpt.isPresent()) {
                GeneratedQuiz cached = cachedOpt.get();
                List<Map<String, Object>> cachedQuestions = mapper.readValue(
                        cached.getQuestionsJson(),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});

                Map<String, Object> cachedResponse = new LinkedHashMap<>();
                cachedResponse.put("topic", topic);
                cachedResponse.put("difficulty", adaptiveDiff);
                cachedResponse.put("questionsGenerated", cachedQuestions.size());
                cachedResponse.put("generationTimeMs", Instant.now().toEpochMilli() - start.toEpochMilli());
                cachedResponse.put("cached", true);
                cachedResponse.put("questions", cachedQuestions);
                cachedResponse.put("quizId", cached.getId());

                log.info("♻️ Cache HIT | topic='{}' | difficulty='{}' | quizId={} — skipped LLM call",
                        topic, adaptiveDiff, cached.getId());
                return ResponseEntity.ok(cachedResponse);
            }

            // 🧩 4. Generate questions (cache miss)
            var questions = llmService.generateQuestions(topic, adaptiveDiff, context, questionCount);

            // 🧩 5. Persist generated quiz
            GeneratedQuiz generatedQuiz = null;
            if (user != null) {
                String json = mapper.writeValueAsString(questions);
                generatedQuiz = new GeneratedQuiz();
                generatedQuiz.setUser(user);
                generatedQuiz.setTopic(topic);
                generatedQuiz.setDifficulty(adaptiveDiff);
                generatedQuiz.setQuestionsJson(json);
                generatedQuiz.setContentHash(contentHash);
                generatedQuizRepository.save(generatedQuiz);
            }

            // 🧩 5. Response
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("topic", topic);
            response.put("difficulty", adaptiveDiff);
            response.put("questionsGenerated", questions.size());
            response.put("generationTimeMs", Instant.now().toEpochMilli() - start.toEpochMilli());
            response.put("questions", questions);
            if (generatedQuiz != null) response.put("quizId", generatedQuiz.getId());

            log.info("✅ AI Quiz generated | topic='{}' | difficulty='{}' | Qs={} | {} ms",
                    topic, adaptiveDiff, questions.size(), Instant.now().toEpochMilli() - start.toEpochMilli());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ AI Quiz generation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/submit")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> submitQuiz(@RequestBody Map<String, Object> payload, Principal principal) {
        try {
            Long quizId = payload.containsKey("quizId") ? ((Number) payload.get("quizId")).longValue() : null;
            String topic = (String) payload.getOrDefault("topic", "General");
            String difficulty = (String) payload.getOrDefault("difficulty", "medium");
            List<Map<String, Object>> answers = (List<Map<String, Object>>) payload.get("answers");
            int timeTaken = (int) payload.getOrDefault("timeTaken", 0);

            if (answers == null || answers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Answers cannot be empty"));
            }

            User user = (principal != null)
                    ? userRepository.findByUsername(principal.getName()).orElse(null)
                    : null;

            int correctCount = 0;
            int total = answers.size();

            // ✅ Secure evaluation using DB-stored quiz JSON
            if (quizId != null) {
                var quizOpt = generatedQuizRepository.findById(quizId);
                if (quizOpt.isPresent()) {
                    var quiz = quizOpt.get();
                    List<Map<String, Object>> storedQuestions = mapper.readValue(
                            quiz.getQuestionsJson(),
                            new com.fasterxml.jackson.core.type.TypeReference<>() {}
                    );

                    for (int i = 0; i < Math.min(storedQuestions.size(), answers.size()); i++) {
                        var userAnswer = answers.get(i);
                        Object selObj = userAnswer.get("selectedIndex");
                        if (selObj == null) continue;

                        int selectedIndex;
                        try {
                            selectedIndex = Integer.parseInt(selObj.toString());
                        } catch (NumberFormatException e) {
                            continue;
                        }

                        Map<String, Object> q = storedQuestions.get(i);
                        List<String> options = (List<String>) q.get("options");
                        String correctAnswer = (String) q.get("answer");

                        if (options == null || selectedIndex < 0 || selectedIndex >= options.size()) continue;

                        String selectedText = options.get(selectedIndex).trim();
                        String normalizedAnswer = correctAnswer.trim();

                        boolean isCorrect = false;

                        // ✅ Handle "Option A/B/C/D"
                        if (normalizedAnswer.matches("(?i)^option\\s*[A-D]$")) {
                            int correctIndex = Character.toUpperCase(
                                    normalizedAnswer.charAt(normalizedAnswer.length() - 1)) - 'A';
                            isCorrect = (selectedIndex == correctIndex);
                        }
                        // ✅ Handle "A)", "B)" type
                        else if (normalizedAnswer.matches("(?i)^[A-D]\\)$")) {
                            int correctIndex = Character.toUpperCase(normalizedAnswer.charAt(0)) - 'A';
                            isCorrect = (selectedIndex == correctIndex);
                        }
                        // ✅ Handle full-text match
                        else {
                            isCorrect = selectedText.equalsIgnoreCase(normalizedAnswer)
                                    || selectedText.contains(normalizedAnswer)
                                    || normalizedAnswer.contains(selectedText);
                        }

                        if (isCorrect) {
                            correctCount++;
                            log.debug("✅ Q{} Correct → selected='{}' | answer='{}'", i + 1, selectedText, correctAnswer);
                        } else {
                            log.debug("❌ Q{} Wrong → selected='{}' | answer='{}'", i + 1, selectedText, correctAnswer);
                        }
                    }
                } else {
                    log.warn("⚠️ QuizId={} not found — fallback evaluation", quizId);
                    correctCount = (int) answers.stream().filter(a -> Boolean.TRUE.equals(a.get("isCorrect"))).count();
                }
            } else {
                // fallback for static quizzes
                correctCount = (int) answers.stream().filter(a -> Boolean.TRUE.equals(a.get("isCorrect"))).count();
            }

            int scorePercent = (int) Math.round((correctCount * 100.0) / total);

            // ✅ Save attempt after evaluation
            if (user != null) {
                QuizAttempt attempt = new QuizAttempt(user, topic, difficulty, scorePercent, timeTaken);
                if (quizId != null) {
                    generatedQuizRepository.findById(quizId).ifPresent(attempt::setGeneratedQuiz);
                }
                quizAttemptRepository.save(attempt);
                analyticsService.updateGamification(user, scorePercent);
            }

            String nextLevel = computeNextLevel(scorePercent, difficulty);

            log.info("✅ Secure evaluation completed | topic='{}' | score={} | correct={} | total={} | next='{}'",
                    topic, scorePercent, correctCount, total, nextLevel);

            return ResponseEntity.ok(Map.of(
                    "scorePercent", scorePercent,
                    "correct", correctCount,
                    "total", total,
                    "nextDifficulty", nextLevel
            ));
        } catch (Exception e) {
            log.error("❌ Quiz submission failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Submission failed",
                    "details", e.getMessage()
            ));
        }
    }

    // -------------------------------------------------------------------------
    // 🧩 3️⃣ User Quiz History
    // -------------------------------------------------------------------------
    @GetMapping("/attempts/{userId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getUserAttempts(@PathVariable Long userId) {
        try {
            var attempts = quizAttemptRepository.findByUserId(userId);
            if (attempts.isEmpty()) return ResponseEntity.ok(List.of());

            var result = attempts.stream().map(a -> Map.of(
                    "id", a.getId(),
                    "topic", a.getTopic(),
                    "score", a.getScore(),
                    "difficulty", a.getDifficultyLevel(),
                    "date", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "N/A"
            )).toList();

            log.info("📜 Returned {} quiz attempts for user {}", result.size(), userId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Failed to fetch quiz attempts: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // 🔑 Content fingerprint for quiz caching (SHA-256)
    // -------------------------------------------------------------------------
    private String computeContentHash(String context, String topic, String difficulty, int questionCount) {
        try {
            String seed = (topic == null ? "" : topic.trim().toLowerCase()) + "|"
                    + (difficulty == null ? "" : difficulty.trim().toLowerCase()) + "|"
                    + questionCount + "|"
                    + (context == null ? "" : context);
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            log.warn("⚠️ Failed to compute content hash, caching disabled for this request: {}", e.getMessage());
            return null; // null hash → lookup misses, behaves like no cache
        }
    }

    // -------------------------------------------------------------------------
    // 🔁 Adaptive Level Calculation
    // -------------------------------------------------------------------------
    private String computeNextLevel(int scorePercent, String currentDifficulty) {
        if (scorePercent >= 85 && !"hard".equalsIgnoreCase(currentDifficulty)) return "hard";
        if (scorePercent <= 50 && !"easy".equalsIgnoreCase(currentDifficulty)) return "easy";
        return currentDifficulty;
    }

    // -------------------------------------------------------------------------
    // 🧩 4️⃣ List All Generated Quizzes
    // -------------------------------------------------------------------------
    @GetMapping("/list")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> listAllGeneratedQuizzes() {
        try {
            var quizzes = generatedQuizRepository.findAll();
            if (quizzes.isEmpty()) return ResponseEntity.ok(List.of());

            var result = quizzes.stream().map(q -> Map.of(
                    "id", q.getId(),
                    "topic", q.getTopic(),
                    "difficulty", q.getDifficulty(),
                    "user", q.getUser() != null ? q.getUser().getUsername() : "Anonymous",
                    "questionCount", countQuestions(q.getQuestionsJson()),
                    "createdAt", q.getGeneratedAt() != null ? q.getGeneratedAt().toString() : "N/A"
            )).toList();

            log.info("📋 Listed {} generated quizzes.", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ Failed to fetch generated quizzes: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private int countQuestions(String json) {
        try {
            var questions = mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            return questions.size();
        } catch (Exception e) {
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // 🧠 5️⃣ Get Full Quiz by ID (with answers)
    // -------------------------------------------------------------------------
    @GetMapping("/get/{quizId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getGeneratedQuiz(@PathVariable Long quizId) {
        try {
            var quizOpt = generatedQuizRepository.findById(quizId);
            if (quizOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Quiz not found for id=" + quizId));
            }

            var quiz = quizOpt.get();
            List<Map<String, Object>> questions = mapper.readValue(
                    quiz.getQuestionsJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<>() {}
            );

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("quizId", quiz.getId());
            response.put("topic", quiz.getTopic());
            response.put("difficulty", quiz.getDifficulty());
            response.put("user", quiz.getUser() != null ? quiz.getUser().getUsername() : "Anonymous");
            response.put("createdAt", quiz.getGeneratedAt());
            response.put("questionCount", questions.size());
            response.put("questions", questions);

            log.info("📘 Returned quizId={} | topic='{}' | difficulty='{}' | {} questions",
                    quiz.getId(), quiz.getTopic(), quiz.getDifficulty(), questions.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Failed to fetch quiz {}: {}", quizId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to fetch quiz",
                    "details", e.getMessage()
            ));
        }
    }
}
