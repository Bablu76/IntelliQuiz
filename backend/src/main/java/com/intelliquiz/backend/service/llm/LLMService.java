package com.intelliquiz.backend.service.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

/**
 * 🧠 LLMService — Adaptive Gemini/OpenAI Quiz Generator
 *
 * ✅ Supports multi-model Gemini family (2.5-pro, 2.5-flash, 2.5-flash-lite)
 * ✅ Auto-fallback if a model fails or API returns NOT_FOUND
 * ✅ Intelligent chunking and context handling for long PDFs
 * ✅ Resilient structured parsing + graceful fallbacks
 */
@Service
public class LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMService.class);

    // Provider and API keys
    @Value("${llm.provider:gemini}")
    private String provider;

    @Value("${openai.api.key:}")
    private String openAIApiKey;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    // Configurable model preferences (order of fallback)
    @Value("${gemini.model.primary:models/gemini-2.5-flash}")
    private String geminiPrimaryModel;

    @Value("${gemini.model.secondary:models/gemini-2.5-pro}")
    private String geminiSecondaryModel;

    @Value("${gemini.model.tertiary:models/gemini-2.5-flash-lite}")
    private String geminiTertiaryModel;

    // Timeout config (ms)
    private static final int CONNECT_TIMEOUT = 6000;
    private static final int READ_TIMEOUT = 30000;
    private static final int MAX_CHARS_PER_CHUNK = 200000; // ~50k tokens

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public LLMService() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(CONNECT_TIMEOUT);
        rf.setReadTimeout(READ_TIMEOUT);
        this.restTemplate = new RestTemplate(rf);
    }

    // ---------------------------------------------------------------
    // 🧠 Entry Point: Generate Questions
    // ---------------------------------------------------------------
    public List<Map<String, Object>> generateQuestions(String topic, String difficulty, String pdfContext, int questionCount) {
        Instant start = Instant.now();
        if (pdfContext == null || pdfContext.isBlank()) {
            log.warn("⚠️ Empty context for '{}', returning fallback.", topic);
            return generateFallbackQuestions(topic, difficulty, questionCount);
        }

        String effectiveProvider = decideProvider();
        log.info("🤖 Provider={} | topic='{}' | difficulty='{}' | chars={} | questions={}",
                effectiveProvider, topic, difficulty, pdfContext.length(), questionCount);

        List<String> chunks = chunkTextSmart(pdfContext, MAX_CHARS_PER_CHUNK);
        List<Map<String, Object>> allQuestions = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String prompt = buildPrompt(topic, difficulty, chunk, questionCount / chunks.size());
            log.info("📄 Sending chunk {}/{} ({} chars)", i + 1, chunks.size(), chunk.length());

            try {
                String response = "openai".equalsIgnoreCase(effectiveProvider)
                        ? callOpenAI(prompt)
                        : callGeminiAdaptive(prompt);

                List<Map<String, Object>> questions = parseQuestions(response);
                allQuestions.addAll(questions);
                log.info("✅ Chunk {}/{} generated {} questions", i + 1, chunks.size(), questions.size());

            } catch (Exception e) {
                log.error("❌ Chunk {}/{} failed: {}", i + 1, chunks.size(), e.getMessage());
                allQuestions.addAll(generateFallbackQuestions(topic, difficulty, Math.max(1, questionCount / chunks.size())));
            }
        }

        long latency = Instant.now().toEpochMilli() - start.toEpochMilli();
        log.info("🧩 LLM complete → totalQuestions={} | chunks={} | latency={}ms", allQuestions.size(), chunks.size(), latency);

        return allQuestions.isEmpty()
                ? generateFallbackQuestions(topic, difficulty, questionCount)
                : allQuestions;
    }

    // ---------------------------------------------------------------
    // 🧩 Prompt Builder + Smart Chunking
    // ---------------------------------------------------------------
    private String buildPrompt(String topic, String difficulty, String context, int questionCount) {
        return String.format("""
                You are an AI Quiz Generator for IntelliQuiz.
                Create %d multiple-choice questions based ONLY on this context:
                "%s"
                Topic: "%s"
                Difficulty: %s

                Format:
                [{"question":"...","options":["A","B","C","D"],"answer":"B"}]
                """, Math.max(1, questionCount), context, topic, difficulty);
    }

    private List<String> chunkTextSmart(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('.', end);
                if (lastPeriod > start + 3000) end = lastPeriod + 1;
            }
            chunks.add(text.substring(start, end).trim());
            start = end;
        }
        log.info("🧠 Split text into {} chunks (max {} chars)", chunks.size(), chunkSize);
        return chunks;
    }

    // ---------------------------------------------------------------
    // ☁️ Gemini & OpenAI Integrations
    // ---------------------------------------------------------------
    private String callOpenAI(String prompt) {
        if (openAIApiKey == null || openAIApiKey.isBlank())
            throw new IllegalStateException("OpenAI API key missing");

        String url = "https://api.openai.com/v1/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAIApiKey);

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "temperature", 0.2,
                "max_tokens", 1500,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null)
            throw new RestClientException("OpenAI returned " + response.getStatusCode());

        Object choicesObj = response.getBody().get("choices");
        if (choicesObj instanceof List<?> list && !list.isEmpty()) {
            Map<?, ?> choice = (Map<?, ?>) list.get(0);
            Object msg = choice.get("message");
            if (msg instanceof Map<?, ?> map && map.get("content") instanceof String s)
                return s;
        }
        throw new RestClientException("Invalid OpenAI response");
    }

    /**
     * Calls Gemini with adaptive model fallback
     */
    private String callGeminiAdaptive(String prompt) {
        List<String> modelPriority = List.of(geminiPrimaryModel, geminiSecondaryModel, geminiTertiaryModel);

        for (String model : modelPriority) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/" + model + ":generateContent?key=" + geminiApiKey;
                log.debug("🧠 Trying Gemini model: {}", model);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                Map<String, Object> req = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
                ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(req, headers), Map.class);

                if (res.getStatusCode() == HttpStatus.OK && res.getBody() != null) {
                    Object candidates = res.getBody().get("candidates");
                    if (candidates instanceof List<?> list && !list.isEmpty()) {
                        Map<?, ?> candidate = (Map<?, ?>) list.get(0);
                        Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                        List<?> parts = (List<?>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            Object text = ((Map<?, ?>) parts.get(0)).get("text");
                            if (text instanceof String s) {
                                log.info("✅ Gemini response successful via model '{}'", model);
                                return s;
                            }
                        }
                    }
                }

                log.warn("⚠️ Model '{}' returned no usable content", model);

            } catch (Exception ex) {
                log.warn("⚠️ Gemini model '{}' failed: {}", model, ex.getMessage());
            }
        }

        throw new RestClientException("All Gemini models failed");
    }

    // ---------------------------------------------------------------
    // 🧠 Helpers
    // ---------------------------------------------------------------
    private String decideProvider() {
        if ("openai".equalsIgnoreCase(provider)) {
            return (openAIApiKey == null || openAIApiKey.isBlank()) ? "gemini" : "openai";
        }
        return (geminiApiKey == null || geminiApiKey.isBlank()) ? "openai" : "gemini";
    }

    private List<Map<String, Object>> parseQuestions(String json) throws Exception {
        if (json == null || json.isBlank()) throw new IllegalArgumentException("Empty response");
        try {
            return mapper.readValue(json.trim(), new TypeReference<>() {});
        } catch (Exception e) {
            int s = json.indexOf('['), eIdx = json.lastIndexOf(']');
            if (s >= 0 && eIdx > s) return mapper.readValue(json.substring(s, eIdx + 1), new TypeReference<>() {});
            throw new IllegalArgumentException("Failed to parse JSON");
        }
    }

    private List<Map<String, Object>> generateFallbackQuestions(String topic, String difficulty, int count) {
        List<Map<String, Object>> fallback = new ArrayList<>();
        for (int i = 1; i <= Math.max(1, count); i++) {
            fallback.add(Map.of(
                    "question", String.format("[%s] Sample question %d about %s?", difficulty, i, topic),
                    "options", List.of("Option A", "Option B", "Option C", "Option D"),
                    "answer", "Option A"
            ));
        }
        log.info("🛡️ Returning {} fallback questions for '{}'", fallback.size(), topic);
        return fallback;
    }
}
