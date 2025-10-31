package com.intelliquiz.backend.security.services;

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
 * ContentQuizService
 * - Extracts text from uploaded PDF resources (local file system)
 * - Generates simple deterministic MCQs from extracted sentences
 *
 * Note: this is an "NLP-ready" deterministic generator suitable for demo.
 * Later you can replace generateQuestionFromText() to call an LLM.
 */
@Service
public class ContentQuizService {

    private static final Logger log = LoggerFactory.getLogger(ContentQuizService.class);

    private final ResourceRepository resourceRepository;

    // folder where files are stored; should match application.properties file.upload-dir
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public ContentQuizService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    /**
     * Generate questions from a resource matching topic (case-insensitive).
     * Returns an empty list if no resource found or parsing fails.
     */
    public List<Map<String, Object>> generateQuestionsFromTopic(String topic, String difficulty) {
        List<Map<String, Object>> questions = new ArrayList<>();

        try {
            Optional<Resource> resOpt = resourceRepository.findByTopicIgnoreCase(topic);
            if (resOpt.isEmpty()) {
                log.info("No resource found for topic: {}", topic);
                return Collections.emptyList();
            }

            Resource resource = resOpt.get();
            String storedFileName = resource.getFileName(); // stored filename in uploads
            File pdfFile = new File(uploadDir, storedFileName);

            if (!pdfFile.exists()) {
                log.warn("Resource file not found on disk: {}", pdfFile.getAbsolutePath());
                return Collections.emptyList();
            }

            String fullText = extractTextFromPdf(pdfFile);
            if (fullText == null || fullText.isBlank()) {
                log.warn("Parsed PDF contains no text: {}", pdfFile.getName());
                return Collections.emptyList();
            }

            // Basic sentence split — simple heuristic
            List<String> sentences = splitIntoSentences(fullText);

            // We want at least 3 sentences to form a question + distractors
            if (sentences.size() < 3) {
                log.warn("Not enough sentences ({}) to form questions for topic {}", sentences.size(), topic);
                return Collections.emptyList();
            }

            // Pick up to 5 questions from different parts of the document
            int maxQuestions = Math.min(5, Math.max(1, sentences.size() / 5)); // e.g., 5% of sentences or up to 5
            // but ensure at least 3 questions for better demo:
            maxQuestions = Math.min(5, Math.max(1, Math.min(5, sentences.size()/Math.max(1, sentences.size()/3))));

            // Simpler approach: sample sentences at intervals to cover the doc
            int interval = Math.max(1, sentences.size() / Math.max(1, maxQuestions));
            int qid = 1;
            for (int i = 0; i < sentences.size() && qid <= maxQuestions; i += interval, qid++) {
                String correctSentence = sentences.get(i).trim();
                // Create a short question from the sentence:
                String questionText = createQuestionFromSentence(correctSentence, topic);

                // Build options: correct answer (a key phrase) and 3 distractors sampled from other sentences
                String correctAnswer = excerptKeywordFromSentence(correctSentence);
                List<String> distractors = pickDistractors(sentences, i, 3);

                // Ensure unique options and shuffle
                Set<String> optionsSet = new LinkedHashSet<>();
                optionsSet.add(correctAnswer);
                optionsSet.addAll(distractors);

                // If not enough unique distractors, fill with generic placeholders
                while (optionsSet.size() < 4) {
                    optionsSet.add("Option " + (optionsSet.size() + 1));
                }

                List<String> options = new ArrayList<>(optionsSet).subList(0, 4);
                Collections.shuffle(options);

                Map<String, Object> q = new HashMap<>();
                q.put("questionId", qid);
                q.put("question", questionText);
                q.put("options", options);
                q.put("answer", correctAnswer);

                questions.add(q);
            }

            log.info("Generated {} questions for topic '{}' using resource '{}'", questions.size(), topic, resource.getFileName());
            return questions;

        } catch (Exception e) {
            log.error("Error generating questions for topic {}: {}", topic, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String extractTextFromPdf(File file) {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("PDF parsing failed for file {}: {}", file.getAbsolutePath(), e.getMessage(), e);
            return null;
        }
    }

    private List<String> splitIntoSentences(String text) {
        // Very simple sentence split based on dot, question mark, exclamation
        // For production, use OpenNLP or a proper sentence splitter
        String[] raw = text.split("(?<=[\\.\\?\\!])\\s+");
        return Arrays.stream(raw)
                .map(String::trim)
                .filter(s -> s.length() > 20) // ignore very short sentences
                .collect(Collectors.toList());
    }

    private String createQuestionFromSentence(String sentence, String topic) {
        // Very naive — transform sentence into a question form
        // Example: "Supervised learning is the process of..." -> "What is supervised learning?"
        // Strategy: If sentence contains " is " or " are ", split and rephrase.
        String lower = sentence.toLowerCase();
        if (lower.contains(" is ")) {
            String[] parts = sentence.split("\\s+is\\s+", 2);
            if (parts.length >= 1) {
                String subject = parts[0].replaceAll("\\b(that|which|who)\\b", "").trim();
                return "What is " + tidySubject(subject) + "?";
            }
        }
        if (lower.contains(" are ")) {
            String[] parts = sentence.split("\\s+are\\s+", 2);
            String subject = parts[0].trim();
            return "What are " + tidySubject(subject) + "?";
        }
        // fallback: ask "Which statement is true about <topic>?"
        return "Which statement is true about " + (topic == null ? "this topic" : topic) + "?";
    }

    private String tidySubject(String s) {
        // remove trailing commas/phrases
        String out = s.replaceAll("[^A-Za-z0-9\\s]", "").trim();
        if (out.length() > 40) out = out.substring(0, 40) + "...";
        return out;
    }

    private String excerptKeywordFromSentence(String sentence) {
        // pick a short phrase or first noun chunk as 'answer'
        // naive: pick first 4-8 words (or part before comma)
        String cleaned = sentence.replaceAll("\\s+", " ").trim();
        if (cleaned.contains(",")) cleaned = cleaned.split(",")[0];
        String[] parts = cleaned.split("\\s+");
        int len = Math.min(parts.length, 6);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(parts[i]);
            if (i < len - 1) sb.append(" ");
        }
        String ans = sb.toString();
        if (ans.length() > 80) ans = ans.substring(0, 80) + "...";
        return ans;
    }

    private List<String> pickDistractors(List<String> sentences, int excludeIndex, int needed) {
        List<String> pool = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            if (i == excludeIndex) continue;
            String candidate = excerptKeywordFromSentence(sentences.get(i));
            if (candidate != null && candidate.length() > 4) pool.add(candidate);
        }
        Collections.shuffle(pool);
        // choose top 'needed' unique items
        LinkedHashSet<String> chosen = new LinkedHashSet<>();
        for (String s : pool) {
            if (chosen.size() >= needed) break;
            chosen.add(s);
        }
        return new ArrayList<>(chosen);
    }
}
