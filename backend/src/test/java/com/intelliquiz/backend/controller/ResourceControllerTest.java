package com.intelliquiz.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelliquiz.backend.model.Resource;
import com.intelliquiz.backend.model.Role;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.repository.UserRepository;
import com.intelliquiz.backend.security.services.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourceController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User teacher;
    private Resource resource;

    @BeforeEach
    void setup() {
        teacher = new User();
        teacher.setId(1L);
        teacher.setUsername("teacher1");
        Role role = new Role();
        role.setName(com.intelliquiz.backend.model.ERole.ROLE_TEACHER);
        teacher.setRoles(Set.of(role));

        resource = new Resource();
        resource.setId(99L);
        resource.setFileName("notes.pdf");
        resource.setTopic("AI");
        resource.setUploader(teacher);
        resource.setUploadedAt(LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void uploadResource_returnsSuccess() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "notes.pdf", "application/pdf", "dummy".getBytes());

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(teacher));
        when(resourceService.saveResource(any(), any(), anyString())).thenReturn(resource);

        mockMvc.perform(multipart("/resources/upload")
                        .file(file)
                        .param("topic", "AI")
                        .principal(() -> "teacher1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File uploaded successfully"));

        verify(resourceService, times(1)).saveResource(any(), eq(teacher), eq("AI"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void listUserResources_returnsList() throws Exception {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(teacher));
        when(resourceService.getUserResources(1L)).thenReturn(List.of(resource));

        mockMvc.perform(get("/resources/list").principal(() -> "teacher1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("notes.pdf"));

        verify(resourceService, times(1)).getUserResources(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAllResources_returnsAll() throws Exception {
        when(resourceService.getAllResources()).thenReturn(List.of(resource));

        mockMvc.perform(get("/resources/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].topic").value("AI"));

        verify(resourceService, times(1)).getAllResources();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void deleteResource_returnsSuccessMessage() throws Exception {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(teacher));
        doNothing().when(resourceService).deleteResource(eq(99L), eq(teacher));

        mockMvc.perform(delete("/resources/{id}", 99L).principal(() -> "teacher1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Resource deleted successfully"));

        verify(resourceService, times(1)).deleteResource(99L, teacher);
    }
}
