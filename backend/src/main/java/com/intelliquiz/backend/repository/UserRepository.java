package com.intelliquiz.backend.repository;

import com.intelliquiz.backend.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 🔹 Authentication and lookup (keep intact)
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    // 🏆 Leaderboard — top users by points (paged)
    @Query("SELECT u FROM User u ORDER BY u.points DESC")
    List<User> findTopUsers(Pageable pageable);

    // 🧠 Role-based leaderboard (optional)
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName ORDER BY u.points DESC")
    List<User> findTopByRoleName(@Param("roleName") String roleName, Pageable pageable);

    // 👨‍🏫 Fetch all teachers (role-based view)
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_TEACHER'")
    List<User> findAllTeachers();

    // 👨‍🎓 Fetch all students
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_STUDENT'")
    List<User> findAllStudents();

    // 🏅 Legacy method (still safe to keep if used elsewhere)
    @Query("SELECT u FROM User u ORDER BY u.points DESC")
    List<User> findTopPerformers(); // kept for backward compatibility
}
