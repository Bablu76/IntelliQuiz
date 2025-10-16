package com.intelliquiz.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/classroom")
public class ClassroomController {

    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getClassrooms() {
        List<Map<String, Object>> classrooms = new ArrayList<>();
        classrooms.add(Map.of("id", 1, "name", "Math 101"));
        classrooms.add(Map.of("id", 2, "name", "History 201"));
        return ResponseEntity.ok(classrooms);
    }
}
