package com.intelliquiz.backend.repository;

import com.intelliquiz.backend.model.QuizAttempt;
import com.intelliquiz.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByUser(User user);

    // 🧠 Fetch most recent attempts (for adaptive difficulty)
    @Query("SELECT q FROM QuizAttempt q WHERE q.user.id = :userId ORDER BY q.createdAt DESC")
    List<QuizAttempt> findRecentAttempts(@Param("userId") Long userId);

    // 📊 Average score for analytics
    @Query("SELECT AVG(q.score) FROM QuizAttempt q WHERE q.user.id = :userId")
    Double findAverageScore(@Param("userId") Long userId);

    // 📈 Fetch last five scores (for performance trend)
    @Query("SELECT q.score FROM QuizAttempt q WHERE q.user.id = :userId ORDER BY q.createdAt DESC")
    List<Integer> findLastFiveScores(@Param("userId") Long userId);
}
