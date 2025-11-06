package com.intelliquiz.backend.repository;

import com.intelliquiz.backend.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    // 🔍 Find resource by topic (case-insensitive)
    Optional<Resource> findByTopicIgnoreCase(String topic);

    // 📁 Fetch all resources uploaded by a specific user
    @Query("""
        SELECT r FROM Resource r
        WHERE r.uploader.id = :userId
        ORDER BY r.uploadedAt DESC
    """)
    List<Resource> findByUploaderId(@Param("userId") Long userId);

    // 🧑‍🎓 Fetch all resources uploaded by students
    @Query("""
        SELECT r FROM Resource r
        JOIN r.uploader u
        JOIN u.roles ur
        WHERE ur.name = 'ROLE_STUDENT'
        ORDER BY r.uploadedAt DESC
    """)
    List<Resource> findAllStudentResources();

    // 🧑‍🏫 Fetch all resources uploaded by teachers
    @Query("""
        SELECT r FROM Resource r
        JOIN r.uploader u
        JOIN u.roles ur
        WHERE ur.name = 'ROLE_TEACHER'
        ORDER BY r.uploadedAt DESC
    """)
    List<Resource> findAllTeacherResources();

    // 🔎 Search resources by topic keyword
    @Query("""
        SELECT r FROM Resource r
        WHERE LOWER(r.topic) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY r.uploadedAt DESC
    """)
    List<Resource> searchByTopic(@Param("keyword") String keyword);
}
