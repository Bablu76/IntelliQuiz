package com.intelliquiz.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "users")
@JsonIgnoreProperties({"password", "quizAttempts", "roles"})
public class User {

    // ==============================
    // 🧩 Getters and Setters
    // ==============================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    // Relationship with roles
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    // ==============================
    // 🏆 Gamification Fields
    // ==============================

    @Column(nullable = false)
    private int points = 0; // Total points earned

    @Column(length = 1000)
    private String badges = ""; // Comma-separated badge titles

    // ==============================
    // 📊 Relationship with QuizAttempts
    // ==============================

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuizAttempt> quizAttempts = new ArrayList<>();

    // ==============================
    // ⚙️ Constructors
    // ==============================
    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

}
