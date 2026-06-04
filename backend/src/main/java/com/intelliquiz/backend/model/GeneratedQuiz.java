package com.intelliquiz.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "generated_quizzes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String topic;
    private String difficulty;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String questionsJson;

    // 🔑 Fingerprint of (content + topic + difficulty + questionCount).
    // Lets us reuse a previously generated quiz instead of paying for the LLM again.
    @Column(length = 64)
    private String contentHash;

    private Instant generatedAt = Instant.now();
}
