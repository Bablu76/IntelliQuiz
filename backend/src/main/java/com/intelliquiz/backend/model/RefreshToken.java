package com.intelliquiz.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = false) // ✅ remove unique=true
    private User user;


    @Column(nullable = false,unique=true)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

}
