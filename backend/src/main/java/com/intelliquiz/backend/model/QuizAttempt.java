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

    // ==============================
    // 🧩 Getters and Setters
    // ==============================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int score;

    @Column(nullable = true, length = 255)
    private String topic;


    // Relationship to User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ==============================
    // 🧩 New Fields for Adaptive System
    // ==============================

    @Column(nullable = false)
    private String difficultyLevel = "easy"; // default for first-time quiz

    @Column(nullable = false)
    private int timeTaken = 0; // seconds to complete quiz

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
    // 🧩 Lifecycle Callbacks
    // ==============================
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
