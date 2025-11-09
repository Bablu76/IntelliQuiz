package com.intelliquiz.backend.repository;

import com.intelliquiz.backend.model.GeneratedQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GeneratedQuizRepository extends JpaRepository<GeneratedQuiz, Long> {
    List<GeneratedQuiz> findByUserIdOrderByGeneratedAtDesc(Long userId);
}
