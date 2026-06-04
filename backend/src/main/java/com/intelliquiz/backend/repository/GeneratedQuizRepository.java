package com.intelliquiz.backend.repository;

import com.intelliquiz.backend.model.GeneratedQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GeneratedQuizRepository extends JpaRepository<GeneratedQuiz, Long> {
    List<GeneratedQuiz> findByUserIdOrderByGeneratedAtDesc(Long userId);

    // Cache lookup: reuse the most recent quiz generated for an identical request.
    Optional<GeneratedQuiz> findFirstByContentHashOrderByGeneratedAtDesc(String contentHash);
}
