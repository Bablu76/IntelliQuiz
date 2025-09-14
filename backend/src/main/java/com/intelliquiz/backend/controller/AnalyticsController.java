package com.intelliquiz.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    @GetMapping("/student/{id}")
    public ResponseEntity<Map<String, Object>> getStudentAnalytics(@PathVariable Long id) {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("studentId", id);
        analytics.put("accuracy", "78%");
        analytics.put("weakTopics", List.of("Algebra", "World History"));
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/classroom/{id}")
    public ResponseEntity<Map<String, Object>> getClassroomAnalytics(@PathVariable Long id) {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("classroomId", id);
        analytics.put("averageScore", 65);
        analytics.put("topPerformers", List.of("student1", "student2"));
        return ResponseEntity.ok(analytics);
    }
}
