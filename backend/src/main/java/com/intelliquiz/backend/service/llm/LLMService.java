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

/**
 * 🧠 LLMService — Full-Context Adaptive Quiz Generator
 * Dynamically adjusts chunk size based on context length, model capacity, and target question count.
 * Supports Gemini 2.5 (Flash / Pro / Flash-Lite) and OpenAI GPT-4o-mini.
 */
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
    // 🧠 Generate Questions (Adaptive + Early Stop)
    // ------------------------------------------------------------------------
    public List<Map<String, Object>> generateQuestions(String topic, String difficulty, String fullContext, int totalCount) {
        Instant start = Instant.now();
        if (fullContext == null || fullContext.isBlank()) {
            log.warn("⚠️ Empty context for '{}', returning fallback.", topic);
            return fallback(topic, difficulty, totalCount);
        }

        List<Map<String, Object>> allQuestions = new ArrayList<>();

        int totalChars = fullContext.length();
        int modelLimit = decideModelCharLimit();
        int idealChunkCount = Math.max(1, Math.min(totalCount, (int) Math.ceil((double) totalChars / modelLimit)));

        List<String> chunks = createAdaptiveChunks(fullContext, idealChunkCount);
        log.info("🧩 Context split → {} chunks (~{} chars each) | total={} | targetQs={}",
                chunks.size(), totalChars / chunks.size(), totalChars, totalCount);

        log.info("🧠 LLM started | topic='{}' | difficulty='{}' | chars={} | targetQs={}",
                topic, difficulty, totalChars, totalCount);

        for (int i = 0; i < chunks.size() && allQuestions.size() < totalCount; i++) {
            String chunk = chunks.get(i);
            int remaining = totalCount - allQuestions.size();
            int perChunk = Math.max(1, remaining / (chunks.size() - i));

            String prompt = buildPrompt(topic, difficulty, chunk, perChunk);

            try {
                String response = switch (decideProvider()) {
                    case "openai" -> callOpenAI(prompt);
                    case "gemini" -> callGemini(prompt);
                    default -> throw new IllegalStateException("No valid LLM provider");
                };

                List<Map<String, Object>> parsed = safeParse(response);
                allQuestions.addAll(parsed);

                log.info("✅ Chunk {}/{} → {} questions ({} total so far)",
                        i + 1, chunks.size(), parsed.size(), allQuestions.size());

                if (allQuestions.size() >= totalCount) break;

            } catch (Exception e) {
                log.error("❌ LLM failed on chunk {}/{}: {}", i + 1, chunks.size(), e.getMessage());
                allQuestions.addAll(fallback(topic, difficulty, 1));
            }
        }

        long latency = Instant.now().toEpochMilli() - start.toEpochMilli();
        List<Map<String, Object>> result = allQuestions.size() > totalCount
                ? allQuestions.subList(0, totalCount)
                : allQuestions;

        log.info("🎯 LLM completed | topic='{}' | difficulty='{}' | totalQs={} | latency={}ms",
                topic, difficulty, result.size(), latency);

        return result.isEmpty() ? fallback(topic, difficulty, totalCount) : result;
    }

    // ------------------------------------------------------------------------
    // ✂️ Dynamic Chunking (Adaptive)
    // ------------------------------------------------------------------------
    private List<String> createAdaptiveChunks(String text, int targetChunks) {
        int total = text.length();
        if (targetChunks <= 1 || total <= decideModelCharLimit()) return List.of(text);

        int chunkSize = Math.max(total / targetChunks, 10_000);
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < total; i += chunkSize) {
            chunks.add(text.substring(i, Math.min(i + chunkSize, total)).trim());
        }
        return chunks;
    }

    // ------------------------------------------------------------------------
    // 💬 Prompt Construction
    // ------------------------------------------------------------------------
    private String buildPrompt(String topic, String difficulty, String context, int count) {
        return String.format("""
                You are an expert quiz generator specialized in "%s".
                Based only on the following learning material, create %d conceptual multiple-choice questions.

                Guidelines:
                - Assess understanding of key ideas, reasoning, and real-world applications.
                - Avoid trivial details, names, or publication years.
                - Each question must have 4 options with exactly one correct answer.
                - Ensure questions are clear, meaningful, and relevant to the topic.
                - Difficulty: %s.

                Study Material (excerpt):
                "%s"

                Return strictly valid JSON:
                [
                  {"question":"...","options":["A","B","C","D"],"answer":"Option A"},
                  ...
                ]
                """, topic, count, difficulty, context);
    }

    // ------------------------------------------------------------------------
    // 📏 Model Context Awareness
    // ------------------------------------------------------------------------
    private int decideModelCharLimit() {
        if (geminiPrimaryModel.contains("2.5-flash")) return 3_000_000;     // Gemini 2.5 Flash
        if (geminiPrimaryModel.contains("2.5-pro")) return 2_800_000;       // Gemini 2.5 Pro
        if (geminiPrimaryModel.contains("2.5-flash-lite")) return 2_000_000;// Gemini 2.5 Flash-Lite
        if ("openai".equalsIgnoreCase(provider)) return 1_000_000;          // GPT-4o-mini (~16K tokens)
        return 800_000;
    }

    // ------------------------------------------------------------------------
    // ☁️ Provider Calls
    // ------------------------------------------------------------------------
    private String decideProvider() {
        boolean hasGemini = geminiApiKey != null && !geminiApiKey.isBlank();
        boolean hasOpenAI = openAIApiKey != null && !openAIApiKey.isBlank();

        if ("openai".equalsIgnoreCase(provider) && hasOpenAI) return "openai";
        if ("gemini".equalsIgnoreCase(provider) && hasGemini) return "gemini";
        if (hasGemini) return "gemini";
        if (hasOpenAI) return "openai";
        throw new RuntimeException("No valid LLM provider available.");
    }

    private String callGemini(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/"
                + geminiPrimaryModel + ":generateContent?key=" + geminiApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        Map<?, ?> candidate = (Map<?, ?>) ((List<?>) res.getBody().get("candidates")).get(0);
        Map<?, ?> content = (Map<?, ?>) candidate.get("content");
        List<?> parts = (List<?>) content.get("parts");
        return (String) ((Map<?, ?>) parts.get(0)).get("text");
    }

    private String callOpenAI(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAIApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "temperature", 0.3,
                "max_tokens", 1500,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        return Optional.ofNullable(response.getBody())
                .map(b -> ((List<?>) b.get("choices")).get(0))
                .map(c -> (Map<?, ?>) ((Map<?, ?>) c).get("message"))
                .map(m -> (String) m.get("content"))
                .orElseThrow(() -> new RuntimeException("OpenAI response parsing failed"));
    }

    // ------------------------------------------------------------------------
    // 🧩 Safe Parsing & Fallback
    // ------------------------------------------------------------------------
    private List<Map<String, Object>> safeParse(String json) throws Exception {
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            int s = json.indexOf('[');
            int eIdx = json.lastIndexOf(']');
            if (s >= 0 && eIdx > s)
                return mapper.readValue(json.substring(s, eIdx + 1), new TypeReference<>() {});
            throw e;
        }
    }

    private List<Map<String, Object>> fallback(String topic, String diff, int count) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= Math.max(1, count); i++) {
            list.add(Map.of(
                    "question", String.format("[%s] Backup Question %d about %s?", diff, i, topic),
                    "options", List.of("Option A", "Option B", "Option C", "Option D"),
                    "answer", "Option A"
            ));
        }
        log.warn("🛡️ Fallback used: {} questions for '{}'", list.size(), topic);
        return list;
    }
}
