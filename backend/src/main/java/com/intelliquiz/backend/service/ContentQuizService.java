package com.intelliquiz.backend.service;

import com.intelliquiz.backend.model.Resource;
import com.intelliquiz.backend.repository.ResourceRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🎯 ContentQuizService
 * - Extracts text from stored PDF resources (in uploads folder)
 * - Generates deterministic MCQs from sentences
 * - Falls back to static bank if resource or text missing
 */
@Service
public class ContentQuizService {

    private static final Logger log = LoggerFactory.getLogger(ContentQuizService.class);

    private final ResourceRepository resourceRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public ContentQuizService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    /**
     * Generate questions from uploaded PDF for given topic & difficulty.
     * If no resource found or parsing fails → static fallback bank.
     */
    public List<Map<String, Object>> generateQuestionsFromTopic(String topic, String difficulty) {
        topic = topic == null ? "General" : topic.trim();
        difficulty = difficulty == null ? "medium" : difficulty.trim().toLowerCase();

        log.info("🎯 Generating static questions | topic='{}' difficulty='{}'", topic, difficulty);

        try {
            Optional<Resource> resOpt = resourceRepository.findByTopicIgnoreCase(topic);
            if (resOpt.isEmpty()) {
                log.warn("⚠️ No PDF resource found for topic '{}'. Using fallback questions.", topic);
                return fallbackQuestions(topic, difficulty);
            }

            Resource resource = resOpt.get();
            File pdfFile = new File(uploadDir, resource.getFileName());
            if (!pdfFile.exists()) {
                log.warn("⚠️ PDF file missing at path: {}", pdfFile.getAbsolutePath());
                return fallbackQuestions(topic, difficulty);
            }

            // Extract and clean text
            String text = extractTextFromPdf(pdfFile);
            if (text == null || text.isBlank()) {
                log.warn("⚠️ PDF '{}' has no readable text. Using fallback.", resource.getFileName());
                return fallbackQuestions(topic, difficulty);
            }

            // Split into sentences and generate questions
            List<String> sentences = splitIntoSentences(text);
            if (sentences.size() < 4) {
                log.warn("⚠️ Too few sentences ({}). Using fallback for '{}'.", sentences.size(), topic);
                return fallbackQuestions(topic, difficulty);
            }

            List<Map<String, Object>> questions = generateFromSentences(sentences, topic, difficulty);
            log.info("✅ Generated {} questions from PDF '{}'", questions.size(), resource.getFileName());
            return questions;

        } catch (Exception e) {
            log.error("❌ Error generating questions from topic '{}': {}", topic, e.getMessage(), e);
            return fallbackQuestions(topic, difficulty);
        }
    }

    // ------------------------------------------------------------------------
    // 🔹 Internal logic
    // ------------------------------------------------------------------------

    private String extractTextFromPdf(File file) {
        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc).trim();
            if (text.length() > 50000) { // cap for performance
                text = text.substring(0, 50000);
                log.warn("⚠️ Truncated extracted text to 50k characters for '{}'", file.getName());
            }
            return text;
        } catch (IOException e) {
            log.error("❌ Failed to parse PDF '{}': {}", file.getName(), e.getMessage());
            return null;
        }
    }

    private List<String> splitIntoSentences(String text) {
        String[] raw = text.split("(?<=[.!?])\\s+");
        return Arrays.stream(raw)
                .map(String::trim)
                .filter(s -> s.length() > 30 && Character.isUpperCase(s.charAt(0)))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> generateFromSentences(List<String> sentences, String topic, String difficulty) {
        int questionCount = Math.min(5, Math.max(3, sentences.size() / 10));
        int interval = Math.max(1, sentences.size() / questionCount);

        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0, id = 1; i < sentences.size() && id <= questionCount; i += interval, id++) {
            String sentence = sentences.get(i);
            String question = createQuestionFromSentence(sentence, topic);
            String answer = excerptAnswer(sentence);
            List<String> distractors = pickDistractors(sentences, i, 3);

            Set<String> optionsSet = new LinkedHashSet<>();
            optionsSet.add(answer);
            optionsSet.addAll(distractors);
            while (optionsSet.size() < 4) optionsSet.add("Option " + (optionsSet.size() + 1));

            List<String> options = new ArrayList<>(optionsSet);
            Collections.shuffle(options);

            Map<String, Object> q = new LinkedHashMap<>();
            q.put("questionId", id);
            q.put("question", question);
            q.put("options", options);
            q.put("answer", answer);
            q.put("difficulty", difficulty);
            questions.add(q);
        }

        return questions;
    }

    private String createQuestionFromSentence(String sentence, String topic) {
        String lower = sentence.toLowerCase();
        if (lower.contains(" is ")) {
            String subject = sentence.split("\\s+is\\s+")[0].replaceAll("[^A-Za-z0-9\\s]", "").trim();
            return "What is " + (subject.isBlank() ? topic : subject) + "?";
        }
        if (lower.contains(" are ")) {
            String subject = sentence.split("\\s+are\\s+")[0].replaceAll("[^A-Za-z0-9\\s]", "").trim();
            return "What are " + (subject.isBlank() ? topic : subject) + "?";
        }
        return "Which of the following is true about " + topic + "?";
    }

    private String excerptAnswer(String sentence) {
        String clean = sentence.replaceAll("\\s+", " ").trim();
        if (clean.contains(",")) clean = clean.split(",")[0];
        String[] parts = clean.split("\\s+");
        int len = Math.min(parts.length, 8);
        String answer = String.join(" ", Arrays.copyOfRange(parts, 0, len));
        return answer.length() > 80 ? answer.substring(0, 80) + "..." : answer;
    }

    private List<String> pickDistractors(List<String> sentences, int excludeIndex, int count) {
        List<String> pool = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            if (i == excludeIndex) continue;
            String phrase = excerptAnswer(sentences.get(i));
            if (phrase.length() > 20) pool.add(phrase);
        }
        Collections.shuffle(pool);
        return pool.stream().limit(count).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------
    // 🧩 Static fallback (used if PDF missing or unreadable)
    // ------------------------------------------------------------------------

    private List<Map<String, Object>> fallbackQuestions(String topic, String difficulty) {
        log.info("🛡️ Using fallback static questions for topic='{}'", topic);
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, List<Map<String, Object>>> bank = new HashMap<>();

        bank.put("general-medium", List.of(
                question("Who wrote India's national anthem?",
                        List.of("Rabindranath Tagore", "Mahatma Gandhi", "Nehru", "Bose"), "Rabindranath Tagore"),
                question("Which planet is known as the Red Planet?",
                        List.of("Earth", "Mars", "Jupiter", "Venus"), "Mars")
        ));

        bank.put("ai-medium", List.of(
                question("What is Machine Learning?",
                        List.of("Hardware optimization", "Learning patterns from data", "Manual coding", "Random guessing"),
                        "Learning patterns from data"),
                question("Which algorithm is commonly used for classification?",
                        List.of("Decision Tree", "Bubble Sort", "Quick Sort", "Merge Sort"),
                        "Decision Tree")
        ));

        String key = (topic.toLowerCase() + "-" + difficulty.toLowerCase()).trim();
        list.addAll(bank.getOrDefault(key, genericSamples(topic, difficulty)));

        for (int i = 0; i < list.size(); i++) list.get(i).put("questionId", i + 1);
        return list;
    }

    private Map<String, Object> question(String q, List<String> options, String answer) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("question", q);
        map.put("options", options);
        map.put("answer", answer);
        return map;
    }

    private List<Map<String, Object>> genericSamples(String topic, String difficulty) {
        List<Map<String, Object>> fallback = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            fallback.add(question("Sample question " + i + " about " + topic + "?",
                    List.of("Option A", "Option B", "Option C", "Option D"), "Option A"));
        }
        return fallback;
    }
}
