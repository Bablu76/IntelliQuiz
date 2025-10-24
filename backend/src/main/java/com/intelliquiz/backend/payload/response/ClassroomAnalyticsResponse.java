package com.intelliquiz.backend.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomAnalyticsResponse {
    private Long classroomId;
    private String classroomName;
    private Double averageScore;
    private Double topScore;
    private Integer totalStudents;
}
