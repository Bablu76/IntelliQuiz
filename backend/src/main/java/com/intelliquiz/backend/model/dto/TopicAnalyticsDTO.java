package com.intelliquiz.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicAnalyticsDTO {
    private String topic;
    private double accuracy;
    private long attempts;
}

