package com.intelliquiz.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    // Getters & Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each attempt belongs to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false)
    private int score;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    public QuizAttempt() {}

    public QuizAttempt(User user, String topic, String difficulty, int score) {
        this.user = user;
        this.topic = topic;
        this.difficulty = difficulty;
        this.score = score;
        this.attemptedAt = LocalDateTime.now();
    }

}
