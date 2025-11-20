package com.intelliquiz.backend.service.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
public class LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMService.class);

    @Value("${llm.provider:gemini}")
    private String provider;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${openai.api.key:}")
    private String openAIApiKey;

    @Value("${gemini.model.primary:models/gemini-2.5-flash}")
    private String geminiPrimaryModel;

    // 🔥 Ordered fallback: safest first → most powerful last
    private final List<String> geminiModels = List.of(
            "models/gemini-2.5-flash-lite",
            "models/gemini-2.0-flash",
            "models/gemini-2.5-flash",
            "models/gemini-2.5-flash-preview",
            "models/gemini-3.0-pro-preview",
            "models/gemini-2.5-pro"
    );

    private static final int TIMEOUT = 60000;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public LLMService() {
        var rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(10000);
        rf.setReadTimeout(TIMEOUT);
        this.restTemplate = new RestTemplate(rf);
    }

    // ------------------------------------------------------------------------
    // 🧠 Generate Questions (Main Pipeline)
    // ------------------------------------------------------------------------
    public List<Map<String, Object>> generateQuestions(String topic, String difficulty, String fullContext, int totalCount) {

        Instant start = Instant.now();

        if (fullContext == null || fullContext.isBlank()) {
            log.warn("⚠️ Empty context for topic '{}'", topic);
            return fallback(topic, difficulty, totalCount);
        }

        List<Map<String, Object>> allQuestions = new ArrayList<>();

        List<String> chunks = createFixedChunks(fullContext);
        log.info("🧩 Using fixed chunking → {} chunks of 100k chars", chunks.size());

        for (int i = 0; i < chunks.size() && allQuestions.size() < totalCount; i++) {

            int remaining = totalCount - allQuestions.size();
            int perChunk = Math.max(1, remaining / (chunks.size() - i));

            String prompt = buildPrompt(topic, difficulty, chunks.get(i), perChunk);

            try {
                String raw = callWithFallback(prompt);
                List<Map<String, Object>> parsed = safeParse(raw);
                allQuestions.addAll(parsed);

                log.info("✅ Chunk {}/{} → {} questions ({} total)",
                        i + 1, chunks.size(), parsed.size(), allQuestions.size());

            } catch (Exception e) {
                log.error("❌ LLM chunk {} failed: {}", i + 1, e.getMessage());
                allQuestions.addAll(fallback(topic, difficulty, 1));
            }
        }

        List<Map<String, Object>> finalQs =
                allQuestions.size() > totalCount ?
                        allQuestions.subList(0, totalCount) : allQuestions;

        long latency = Instant.now().toEpochMilli() - start.toEpochMilli();
        log.info("🎯 Completed topic='{}' Qs={} latency={}ms",
                topic, finalQs.size(), latency);

        return finalQs;
    }

    // ------------------------------------------------------------------------
    // ✂️ FIXED 50,000 CHAR CHUNKS
    // ------------------------------------------------------------------------
    private List<String> createFixedChunks(String text) {
        int chunkSize = 100_000;
        List<String> list = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            list.add(text.substring(i, Math.min(i + chunkSize, text.length())).trim());
        }
        return list;
    }

    // ------------------------------------------------------------------------
    // 💬 Prompt builder
    // ------------------------------------------------------------------------
    private String buildPrompt(String topic, String difficulty, String context, int count) {
        return String.format("""
                You are an expert quiz generator specialized in "%s".
                Based on the study material below, create %d high-quality conceptual MCQs.

                Requirements:
                - One correct answer per question
                - Avoid trivia and focus on understanding
                - Provide 4 options in an array
                - Difficulty: %s

                Study Material:
                "%s"

                Return ONLY JSON in this format:
                [
                  {"question":"..","options":["A","B","C","D"],"answer":"Option A"}
                ]
                """, topic, count, difficulty, context);
    }

    // ------------------------------------------------------------------------
    // 🌐 Provider decision + fallback
    // ------------------------------------------------------------------------
    private String callWithFallback(String prompt) throws Exception {

        Exception lastError = null;

        // 1️⃣ Try every Gemini model in order
        for (String model : geminiModels) {

            if (geminiApiKey == null || geminiApiKey.isBlank()) break;

            try {
                log.info("🤖 Trying Gemini model: {}", model);

                String response = callGeminiModel(prompt, model);
                log.info("✅ Gemini model '{}' succeeded", model);
                return response;

            } catch (Exception e) {

                lastError = e;
                String msg = e.getMessage();

                if (msg != null && (msg.contains("429") || msg.contains("503"))) {
                    log.warn("⏳ '{}' rate-limited/overloaded. Trying next model...", model);
                    continue;
                }

                log.warn("⚠️ Gemini '{}' failed (non-rate limit). Trying next...", model);
            }
        }

        // 2️⃣ Try OpenAI as final fallback
        if (openAIApiKey != null && !openAIApiKey.isBlank()) {
            try {
                log.info("🟦 Switching to OpenAI fallback (gpt-4o-mini)");
                return callOpenAI(prompt);
            } catch (Exception e) {
                lastError = e;
            }
        }

        // 3️⃣ All models failed
        throw new RuntimeException("❌ All models failed", lastError);
    }

    // ------------------------------------------------------------------------
    // 🟣 Call Gemini (specific model)
    // ------------------------------------------------------------------------
    private String callGeminiModel(String prompt, String model) {

        String url = "https://generativelanguage.googleapis.com/v1beta/" +
                model + ":generateContent?key=" + geminiApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );

        ResponseEntity<Map> res =
                restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        Map<?, ?> candidate = (Map<?, ?>) ((List<?>) res.getBody().get("candidates")).get(0);
        Map<?, ?> content = (Map<?, ?>) candidate.get("content");
        List<?> parts = (List<?>) content.get("parts");

        return (String) ((Map<?, ?>) parts.get(0)).get("text");
    }

    // ------------------------------------------------------------------------
    // 🟦 Call OpenAI fallback
    // ------------------------------------------------------------------------
    private String callOpenAI(String prompt) {

        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAIApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "temperature", 0.35,
                "max_tokens", 1500,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);

        Map<?, ?> choice = (Map<?, ?>) ((List<?>) response.getBody().get("choices")).get(0);
        Map<?, ?> message = (Map<?, ?>) choice.get("message");
        return (String) message.get("content");
    }

    // ------------------------------------------------------------------------
    // 🧩 JSON Parsing Recovery
    // ------------------------------------------------------------------------
    private List<Map<String, Object>> safeParse(String json) throws Exception {

        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {

            int s = json.indexOf('[');
            int eIdx = json.lastIndexOf(']');

            if (s >= 0 && eIdx > s) {
                return mapper.readValue(json.substring(s, eIdx + 1), new TypeReference<>() {});
            }

            throw e;
        }
    }

    // ------------------------------------------------------------------------
    // 🛡 Built-in fallback for disasters
    // ------------------------------------------------------------------------
    private List<Map<String, Object>> fallback(String topic, String diff, int count) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= Math.max(1, count); i++) {
            list.add(Map.of(
                    "question", "[FALLBACK] Backup Question " + i + " about " + topic,
                    "options", List.of("Option A", "Option B", "Option C", "Option D"),
                    "answer", "Option A"
            ));
        }
        log.warn("🛡️ Fallback used: {} questions", list.size());
        return list;
    }
}
