package com.intelliquiz.backend.security.services;
import com.intelliquiz.backend.model.QuizAttempt;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.repository.QuizAttemptRepository;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class AnalyticsService {
    private final QuizAttemptRepository quizAttemptRepository;

    public AnalyticsService(QuizAttemptRepository repo) {
        this.quizAttemptRepository = repo;
    }

    public List<QuizAttempt> getAttemptsByUser(User user) {
        return quizAttemptRepository.findByUser(user);
    }

    public double calculateAverageScore(User user) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByUser(user);
        return attempts.stream()
                .mapToInt(QuizAttempt::getScore)
                .average()
                .orElse(0.0);
    }
}
