package com.intelliquiz.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 🎯 QuizAttempt Entity
 * Represents a single quiz attempt by a user, including adaptive difficulty.
 */
@Getter
@Setter
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    // ==============================
    // 🔑 Primary Key
    // ==============================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==============================
    // 🧩 Quiz Metadata
    // ==============================
    @Column(nullable = false)
    private int score;

    @Column(length = 255)
    private String topic;

    @Column(nullable = false)
    private String difficultyLevel = "easy"; // ✅ consistent field name

    @Column(nullable = false)
    private int timeTaken = 0; // in seconds

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ==============================
    // 👤 Relationship to User
    // ==============================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ==============================
    // ⚙️ Constructors
    // ==============================
    public QuizAttempt() {}

    public QuizAttempt(User user, String topic, String difficultyLevel, int score, int timeTaken) {
        this.user = user;
        this.topic = topic;
        this.difficultyLevel = difficultyLevel;
        this.score = score;
        this.timeTaken = timeTaken;
    }

    public QuizAttempt(int score, User user, String difficultyLevel, int timeTaken) {
        this.score = score;
        this.user = user;
        this.difficultyLevel = difficultyLevel;
        this.timeTaken = timeTaken;
    }

    // ==============================
    // 🧩 Lifecycle Hook
    // ==============================
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
