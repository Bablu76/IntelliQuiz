package com.intelliquiz.backend.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnalyticsResponse {
    private Long userId;
    private Double averageScore;
    private Double accuracy;
    private List<Integer> trend; // last 5 quiz scores
    private Integer points;
    private List<String> badges;
}
