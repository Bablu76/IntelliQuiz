package com.intelliquiz.backend.service.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ✅ Updated unit tests for LLMService
 * Prevents real API calls and ensures model fields are initialized.
 */
class LLMServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private LLMService llmService;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Inject dummy API config via reflection
        TestUtils.setPrivateField(llmService, "geminiApiKey", "dummy-key");
        TestUtils.setPrivateField(llmService, "geminiPrimaryModel", "models/gemini-2.5-flash");
        TestUtils.setPrivateField(llmService, "provider", "gemini");

        // Override internal RestTemplate (replace new instance created in constructor)
        TestUtils.setPrivateField(llmService, "restTemplate", restTemplate);
    }

    @Test
    void testFallbackTriggeredWhenContextEmpty() {
        List<Map<String, Object>> questions = llmService.generateQuestions("AI", "medium", "", 3);
        assertEquals(3, questions.size());
        assertTrue(questions.get(0).get("question").toString().contains("Backup Question"));
    }

    @Test
    void testAdaptiveChunking_NoAPI() {
        String largeContext = "A".repeat(5000); // shorter, stays single chunk
        List<Map<String, Object>> result = llmService.generateQuestions("ML", "hard", largeContext, 3);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGeminiCallSuccess_Mocked() {
        // Mock Gemini API JSON structure
        Map<String, Object> mockBody = Map.of(
                "candidates", List.of(
                        Map.of("content",
                                Map.of("parts", List.of(
                                        Map.of("text",
                                                "[{\"question\":\"Q1\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"answer\":\"A\"}]"
                                        ))))
                )
        );
        ResponseEntity<Map> fakeResponse = new ResponseEntity<>(mockBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(fakeResponse);

        List<Map<String, Object>> result = llmService.generateQuestions("AI", "easy", "Context text", 1);

        assertEquals(1, result.size());
        assertEquals("Q1", result.get(0).get("question"));
    }

    @Test
    void testSafeParseHandlesPartialJson() throws Exception {
        String messyJson = "garbage [{\"question\":\"Q1\",\"options\":[\"A\"],\"answer\":\"A\"}] end";
        var method = LLMService.class.getDeclaredMethod("safeParse", String.class);
        method.setAccessible(true);

        List<Map<String, Object>> parsed = (List<Map<String, Object>>) method.invoke(llmService, messyJson);
        assertEquals(1, parsed.size());
        assertEquals("Q1", parsed.get(0).get("question"));
    }

    @Test
    void testFallbackGeneratesValidQuestions() throws Exception {
        var method = LLMService.class.getDeclaredMethod("fallback", String.class, String.class, int.class);
        method.setAccessible(true);

        List<Map<String, Object>> list = (List<Map<String, Object>>) method.invoke(llmService, "AI", "medium", 2);
        assertEquals(2, list.size());
        assertTrue(list.get(0).get("options") instanceof List);
    }
}
