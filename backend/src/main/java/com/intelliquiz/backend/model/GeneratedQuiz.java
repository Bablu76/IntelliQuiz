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

    @Column(length = 10000)
    private String questionsJson;

    private Instant generatedAt = Instant.now();
}
