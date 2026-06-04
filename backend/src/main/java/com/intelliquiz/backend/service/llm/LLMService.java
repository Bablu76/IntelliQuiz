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

    // 💰 Cost-controlled fallback: cheapest first, and we deliberately do NOT
    // auto-escalate to expensive "pro" models on a transient rate-limit/overload.
    // Keeping only low-cost "flash" tiers caps the per-quiz token spend.
    private final List<String> geminiModels = List.of(
            "models/gemini-2.5-flash-lite",
            "models/gemini-2.0-flash",
            "models/gemini-2.5-flash"
    );

    // Hard cap on tokens the model may return per call (bounds output cost).
    private static final int MAX_OUTPUT_TOKENS = 2048;

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

        // 💰 Retrieval: instead of shipping the entire document, pick only the
        // passages most relevant to the topic and send them in ONE call. This is
        // the biggest token saver — a 200k-char PDF shrinks to ~MAX_CONTEXT_CHARS.
        String selectedContext = selectRelevantContext(fullContext, topic);
        log.info("🔎 Retrieval: full={} chars → selected={} chars for topic '{}'",
                fullContext.length(), selectedContext.length(), topic);

        List<Map<String, Object>> questions;
        try {
            String prompt = buildPrompt(topic, difficulty, selectedContext, totalCount);
            String raw = callWithFallback(prompt);
            questions = safeParse(raw);
        } catch (Exception e) {
            log.error("❌ LLM generation failed: {}", e.getMessage());
            questions = fallback(topic, difficulty, totalCount);
        }

        List<Map<String, Object>> finalQs =
                questions.size() > totalCount ?
                        new ArrayList<>(questions.subList(0, totalCount)) : questions;

        long latency = Instant.now().toEpochMilli() - start.toEpochMilli();
        log.info("🎯 Completed topic='{}' Qs={} latency={}ms",
                topic, finalQs.size(), latency);

        return finalQs;
    }

    // ------------------------------------------------------------------------
    // 🔎 Lightweight retrieval — keyword-scored passage selection
    // ------------------------------------------------------------------------
    // Tunables: keep the selected context small to bound input tokens.
    private static final int PASSAGE_SIZE = 1200;        // chars per passage
    private static final int MAX_CONTEXT_CHARS = 8000;   // total budget sent to the LLM

    /**
     * Split the document into passages, score each against the topic, and return
     * the highest-scoring passages concatenated up to MAX_CONTEXT_CHARS. If the
     * document already fits the budget, it is returned as-is. If no passage matches
     * the topic, the leading passages are used (preserves original order).
     */
    private String selectRelevantContext(String text, String topic) {
        if (text.length() <= MAX_CONTEXT_CHARS) {
            return text;
        }

        List<String> passages = splitIntoPassages(text);
        Set<String> topicTerms = tokenize(topic);

        // Index preserved so equal scores keep document order (stable selection).
        List<int[]> scored = new ArrayList<>(); // [index, score]
        for (int i = 0; i < passages.size(); i++) {
            scored.add(new int[]{i, scorePassage(passages.get(i), topicTerms)});
        }
        // Sort by score desc, then by original index asc.
        scored.sort((a, b) -> a[1] != b[1] ? Integer.compare(b[1], a[1]) : Integer.compare(a[0], b[0]));

        StringBuilder sb = new StringBuilder();
        for (int[] s : scored) {
            String passage = passages.get(s[0]);
            if (sb.length() + passage.length() + 2 > MAX_CONTEXT_CHARS) break;
            sb.append(passage).append("\n\n");
        }

        String selected = sb.toString().trim();
        // Safety net: if nothing was added (e.g. first passage alone exceeds budget),
        // fall back to a hard slice of the budget.
        return selected.isEmpty()
                ? text.substring(0, Math.min(MAX_CONTEXT_CHARS, text.length()))
                : selected;
    }

    private List<String> splitIntoPassages(String text) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < text.length(); i += PASSAGE_SIZE) {
            list.add(text.substring(i, Math.min(i + PASSAGE_SIZE, text.length())).trim());
        }
        return list;
    }

    /** Number of topic-term occurrences in the passage (case-insensitive). */
    private int scorePassage(String passage, Set<String> topicTerms) {
        if (topicTerms.isEmpty()) return 0;
        String lower = passage.toLowerCase();
        int score = 0;
        for (String term : topicTerms) {
            int idx = 0;
            while ((idx = lower.indexOf(term, idx)) >= 0) {
                score++;
                idx += term.length();
            }
        }
        return score;
    }

    /** Split a topic into lowercase terms longer than 2 chars. */
    private Set<String> tokenize(String topic) {
        Set<String> terms = new LinkedHashSet<>();
        if (topic == null) return terms;
        for (String t : topic.toLowerCase().split("[^a-z0-9]+")) {
            if (t.length() > 2) terms.add(t);
        }
        return terms;
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
                )),
                // Bound output size + keep generation focused → fewer billed tokens.
                "generationConfig", Map.of(
                        "maxOutputTokens", MAX_OUTPUT_TOKENS,
                        "temperature", 0.35
                )
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
