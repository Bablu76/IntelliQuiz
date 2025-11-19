package com.intelliquiz.backend.service.adaptor;

import com.intelliquiz.backend.model.QuizAttempt;
import com.intelliquiz.backend.repository.QuizAttemptRepository;
import com.intelliquiz.backend.service.adaptive.AdaptiveEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdaptiveEngineTest {

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @InjectMocks
    private AdaptiveEngine adaptiveEngine;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSuggestNextDifficulty_NoPastAttempts() {
        when(quizAttemptRepository.findByUserIdAndTopic(1L, "AI"))
                .thenReturn(List.of());

        String next = adaptiveEngine.suggestNextDifficulty(1L, "AI");
        assertEquals("medium", next, "Should default to medium for new users");
    }

    @Test
    void testSuggestNextDifficulty_ImprovingTrend() {
        QuizAttempt a1 = new QuizAttempt(); a1.setScore(60); a1.setDifficultyLevel("medium");
        QuizAttempt a2 = new QuizAttempt(); a2.setScore(85); a2.setDifficultyLevel("medium");

        when(quizAttemptRepository.findByUserIdAndTopic(2L, "ML"))
                .thenReturn(List.of(a1, a2));

        String next = adaptiveEngine.suggestNextDifficulty(2L, "ML");
        assertEquals("hard", next, "Should increase difficulty for improving performance");
    }

    @Test
    void testSuggestNextDifficulty_DecliningTrend() {
        QuizAttempt a1 = new QuizAttempt(); a1.setScore(80); a1.setDifficultyLevel("medium");
        QuizAttempt a2 = new QuizAttempt(); a2.setScore(45); a2.setDifficultyLevel("medium");

        when(quizAttemptRepository.findByUserIdAndTopic(3L, "Python"))
                .thenReturn(List.of(a1, a2));

        String next = adaptiveEngine.suggestNextDifficulty(3L, "Python");
        assertEquals("easy", next, "Should decrease difficulty for declining performance");
    }

    @Test
    void testUpdateAfterSubmission_AdjustmentLogic() {
        String next = adaptiveEngine.updateAfterSubmission(5L, "AI", 90, "medium");
        assertEquals("hard", next);

        next = adaptiveEngine.updateAfterSubmission(5L, "AI", 40, "medium");
        assertEquals("easy", next);

        next = adaptiveEngine.updateAfterSubmission(5L, "AI", 75, "medium");
        assertEquals("medium", next);
    }
}
