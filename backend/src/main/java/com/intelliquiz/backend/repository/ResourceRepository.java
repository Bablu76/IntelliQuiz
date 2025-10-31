package com.intelliquiz.backend.repository;

import com.intelliquiz.backend.model.QuizAttempt;
import com.intelliquiz.backend.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findByTopicIgnoreCase(String topic);

    // 📁 Fetch all resources uploaded by a specific user (teacher or student)
    @Query("SELECT r FROM Resource r WHERE r.uploader.id = :userId ORDER BY r.uploadedAt DESC")
    List<Resource> findByUploaderId(@Param("userId") Long userId);

    @Query("SELECT q FROM QuizAttempt q WHERE q.user.id = :userId ORDER BY q.createdAt DESC")
    List<QuizAttempt> findByUserId(@Param("userId") Long userId);


    // 🧑‍🏫 Fetch all teacher-uploaded resources
    @Query("SELECT r FROM Resource r WHERE r.uploaderRole = 'TEACHER' ORDER BY r.uploadedAt DESC")
    List<Resource> findAllTeacherResources();

    // 👨‍🎓 Fetch all student-uploaded resources
    @Query("SELECT r FROM Resource r WHERE r.uploaderRole = 'STUDENT' ORDER BY r.uploadedAt DESC")
    List<Resource> findAllStudentResources();

    // 🔍 Search by topic keyword (for AI quiz generation later)
    @Query("SELECT r FROM Resource r WHERE LOWER(r.topic) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY r.uploadedAt DESC")
    List<Resource> searchByTopic(@Param("keyword") String keyword);
}
