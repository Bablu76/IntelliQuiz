package com.intelliquiz.backend.security.services;

import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.repository.QuizAttemptRepository;
import com.intelliquiz.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AnalyticsServiceTest {

    private QuizAttemptRepository quizAttemptRepository;
    private UserRepository userRepository;
    private AnalyticsService analyticsService;
    private User user;

    @BeforeEach
    void setup() {
        quizAttemptRepository = mock(QuizAttemptRepository.class);
        userRepository = mock(UserRepository.class);
        analyticsService = new AnalyticsService(quizAttemptRepository, userRepository);

        user = new User();
        user.setId(1L);
        user.setPoints(0);
        user.setBadges("");
    }

    @Test
    void getStudentAnalytics_noAttempts_returnsEmptyTrend() {
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        when(quizAttemptRepository.findAverageScore(1L)).thenReturn(null);
        when(quizAttemptRepository.findLastFiveScores(1L)).thenReturn(List.of());

        var result = analyticsService.getStudentAnalytics(1L);

        assertThat(result.get("averageScore")).isEqualTo(0.0);
        assertThat(((List<?>) result.get("trend")).isEmpty()).isTrue();
    }

    @Test
    void updateGamification_awardsCorrectPoints_and_badgeTransition() {
        analyticsService.updateGamification(user, 90);
        assertThat(user.getPoints()).isEqualTo(50);
        assertThat(user.getBadges()).doesNotContain("Bronze");

        user.setPoints(490);
        analyticsService.updateGamification(user, 90);
        assertThat(user.getBadges()).contains("Silver");
    }

    @Test
    void getLeaderboard_returnsDescendingByPoints() {
        var u1 = new User(); u1.setUsername("A"); u1.setPoints(200);
        var u2 = new User(); u2.setUsername("B"); u2.setPoints(300);
        when(userRepository.findTopUsers(any())).thenReturn(List.of(u2, u1));

        var result = analyticsService.getLeaderboard(2);

        assertThat(result.get(0).get("username")).isEqualTo("B");
    }
}
