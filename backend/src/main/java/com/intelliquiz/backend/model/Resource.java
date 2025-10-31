package com.intelliquiz.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties({"uploader"})
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName; // ✅ renamed for consistency with service

    @Column(nullable = false, length = 50)
    private String fileType; // e.g., "application/pdf"

    @Column(length = 255)
    private String topic;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    // ✅ Many resources per uploader (Student or Teacher)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    @ToString.Exclude
    private User uploader; // ✅ renamed for consistency with service/controller

    @Column(name = "uploader_role", length = 30)
    private String uploaderRole; // e.g., "STUDENT" or "TEACHER"

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
