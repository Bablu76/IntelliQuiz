package com.intelliquiz.backend.security.services;

import com.intelliquiz.backend.model.dto.TopicAnalyticsDTO;
import com.intelliquiz.backend.repository.QuizAttemptRepository;
import com.intelliquiz.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;


public class AnalyticsServiceTest {

    private QuizAttemptRepository quizAttemptRepository;
    private AnalyticsService analyticsService;

    @BeforeEach
    public void setup() {
        quizAttemptRepository = mock(QuizAttemptRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        analyticsService = new AnalyticsService(quizAttemptRepository, userRepository);
    }

    @Test
    public void testGetTopicAnalytics_mapsRowsCorrectly() {
        Long userId = 42L;
        // Simulate repository rows: [topic, avgScore, count]
        Object[] row1 = new Object[] {"AI", 78.5, 4L};
        Object[] row2 = new Object[] {"Math", 62.0, 3L};

        when(quizAttemptRepository.findTopicAveragesByUser(userId)).thenReturn(Arrays.asList(row1, row2));

        List<TopicAnalyticsDTO> result = analyticsService.getTopicAnalytics(userId);
        assertNotNull(result);
        assertEquals(2, result.size());

        TopicAnalyticsDTO first = result.stream().filter(t -> "AI".equals(t.getTopic())).findFirst().orElse(null);
        assertNotNull(first);
        assertEquals("AI", first.getTopic());
        assertEquals(78.5, first.getAccuracy(), 0.01);
        assertEquals(4L, first.getAttempts());

        TopicAnalyticsDTO second = result.stream().filter(t -> "Math".equals(t.getTopic())).findFirst().orElse(null);
        assertNotNull(second);
        assertEquals(62.0, second.getAccuracy(), 0.01);
        assertEquals(3L, second.getAttempts());

        verify(quizAttemptRepository, times(1)).findTopicAveragesByUser(userId);
    }

    @Test
    public void testGetWeakTopics_returnsLowest() {
        Long userId = 1L;
        Object[] row1 = new Object[] {"AI", 90.0, 5L};
        Object[] row2 = new Object[] {"Physics", 40.0, 2L};
        Object[] row3 = new Object[] {"Math", 55.0, 3L};

        when(quizAttemptRepository.findTopicAveragesByUser(userId)).thenReturn(Arrays.asList(row1, row2, row3));

        List<String> weak = analyticsService.getWeakTopics(userId, 2);
        assertNotNull(weak);
        assertEquals(2, weak.size());
        assertEquals("Physics", weak.get(0)); // lowest
        assertEquals("Math", weak.get(1));
    }
}
