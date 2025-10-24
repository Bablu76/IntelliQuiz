package com.intelliquiz.backend.controller;

import com.intelliquiz.backend.security.services.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)

class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private com.intelliquiz.backend.repository.UserRepository userRepository;

    @Test
    void getStudentAnalytics_returns200() throws Exception {
        Mockito.when(userRepository.existsById(1L)).thenReturn(true);
        Mockito.when(analyticsService.getStudentAnalytics(1L))
                .thenReturn(Map.of("userId",1L,"averageScore",90));

        mockMvc.perform(get("/analytics/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").value(90));
    }

    @Test
    void getLeaderboard_returns200() throws Exception {
        Mockito.when(analyticsService.getLeaderboard(5))
                .thenReturn(List.of(Map.of("username","Alice","points",400)));

        mockMvc.perform(get("/analytics/leaderboard?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("Alice"));
    }
}
