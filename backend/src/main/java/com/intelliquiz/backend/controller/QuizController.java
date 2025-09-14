package com.intelliquiz.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> getDummyQuiz() {
        Map<String, Object> quiz = new HashMap<>();
        quiz.put("id", 1);
        quiz.put("title", "Sample Quiz");
        quiz.put("questions", List.of(
                Map.of("q", "What is 2+2?", "options", List.of("3", "4", "5"), "answer", "4"),
                Map.of("q", "Capital of France?", "options", List.of("Berlin", "Paris", "Rome"), "answer", "Paris")
        ));
        return ResponseEntity.ok(quiz);
    }
}
