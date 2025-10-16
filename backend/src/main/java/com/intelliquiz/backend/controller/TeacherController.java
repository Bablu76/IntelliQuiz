package com.intelliquiz.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/teacher")
@CrossOrigin(origins = "http://localhost:5173")
public class TeacherController {

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getTeacherDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Welcome Teacher! Mock dashboard ready.");
        data.put("resourcesCount", 4);
        data.put("classesCount", 2);
        return ResponseEntity.ok(data);
    }
}
