package com.intelliquiz.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelliquiz.backend.repository.GeneratedQuizRepository;
import com.intelliquiz.backend.service.llm.LLMService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 🌐 Integration Test: Quiz Generation + DB persistence (Full pipeline test)
 */
@SpringBootTest
@AutoConfigureMockMvc
class QuizIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LLMService llmService;

    @Autowired
    private GeneratedQuizRepository generatedQuizRepository;

    @Autowired
    private ObjectMapper mapper;

    @Test
    @WithMockUser(username = "teacher1", roles = {"TEACHER"})
    void testGenerateQuizAndSaveToDatabase() throws Exception {

        // Mock AI output from LLMService
        List<Map<String, Object>> mockQuestions = List.of(
                Map.of("question", "What is AI?",
                        "options", List.of("A", "B", "C", "D"),
                        "answer", "A")
        );
        when(llmService.generateQuestions(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(mockQuestions);

        // Valid mock PDF for parsing
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", validMinimalPdf()
        );

        // Perform integration request
        mockMvc.perform(multipart("/quiz/generate/ai")
                        .file(mockFile)
                        .param("topic", "AI")
                        .param("difficulty", "medium")
                        .param("questionCount", "1")
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.questions[0].question").value("What is AI?")); // ✅ fixed path
    }

    /**
     * Valid Minimal PDF accepted by PDFBox.
     */
    private byte[] validMinimalPdf() {
        String pdf = "%PDF-1.4\n" +
                "1 0 obj\n" +
                "<< /Type /Catalog /Pages 2 0 R >>\n" +
                "endobj\n" +
                "2 0 obj\n" +
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n" +
                "endobj\n" +
                "3 0 obj\n" +
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 144] /Contents 4 0 R >>\n" +
                "endobj\n" +
                "4 0 obj\n" +
                "<< /Length 44 >>\n" +
                "stream\n" +
                "BT /F1 24 Tf 72 720 Td (Hello AI Quiz PDF) Tj ET\n" +
                "endstream\n" +
                "endobj\n" +
                "xref\n" +
                "0 5\n" +
                "0000000000 65535 f \n" +
                "0000000010 00000 n \n" +
                "0000000060 00000 n \n" +
                "0000000117 00000 n \n" +
                "0000000217 00000 n \n" +
                "trailer\n" +
                "<< /Root 1 0 R /Size 5 >>\n" +
                "startxref\n" +
                "320\n" +
                "%%EOF";
        return pdf.getBytes();
    }
}
