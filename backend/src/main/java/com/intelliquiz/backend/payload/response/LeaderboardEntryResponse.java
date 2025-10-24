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
public class LeaderboardEntryResponse {
    private String username;
    private String role;      // e.g. STUDENT, TEACHER
    private Integer points;
    private List<String> badges;
}
