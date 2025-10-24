package com.intelliquiz.backend.security.services;

import com.intelliquiz.backend.exception.BadRequestException;
import com.intelliquiz.backend.model.Resource;
import com.intelliquiz.backend.model.Role;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.repository.ResourceRepository;
import com.intelliquiz.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ResourceService resourceService;

    @TempDir
    Path tempDir;

    private User teacherUser;
    private Resource sampleResource;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        teacherUser = new User();
        teacherUser.setId(1L);
        teacherUser.setUsername("teacher1");

        Role teacherRole = new Role();
        teacherRole.setName(com.intelliquiz.backend.model.ERole.ROLE_TEACHER);
        teacherUser.setRoles(Set.of(teacherRole));

        sampleResource = new Resource();
        sampleResource.setId(1L);
        sampleResource.setFileName("test.pdf");
        sampleResource.setUploader(teacherUser);
        sampleResource.setUploaderRole("TEACHER");
        sampleResource.setUploadedAt(LocalDateTime.now());

        // Inject temp upload directory
        resourceService = new ResourceService(resourceRepository, userRepository);
        try {
            var uploadDirField = ResourceService.class.getDeclaredField("uploadDir");
            uploadDirField.setAccessible(true);
            uploadDirField.set(resourceService, tempDir.toString());
        } catch (Exception ignored) {}
    }

    @Test
    void saveResource_savesMetadata() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("sample.pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("PDF-DATA".getBytes()));

        when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> {
            Resource r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });

        Resource saved = resourceService.saveResource(file, teacherUser, "AI Concepts");

        assertThat(saved.getFileName()).contains(".pdf");
        assertThat(saved.getUploader()).isEqualTo(teacherUser);
        assertThat(saved.getTopic()).isEqualTo("AI Concepts");
        verify(resourceRepository, times(1)).save(any(Resource.class));

        Path expectedFile = tempDir.resolve(saved.getFileName());
        assertThat(Files.exists(expectedFile)).isTrue();
    }

    @Test
    void saveResource_throwsBadRequest_forNonPdf() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("notes.txt");
        when(file.getContentType()).thenReturn("text/plain");

        assertThatThrownBy(() -> resourceService.saveResource(file, teacherUser, "AI"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only PDF files are allowed");
    }

    @Test
    void getUserResources_returnsOnlyOwn() {
        when(resourceRepository.findByUploaderId(1L)).thenReturn(List.of(sampleResource));
        List<Resource> list = resourceService.getUserResources(1L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getUploaderRole()).isEqualTo("TEACHER");
        verify(resourceRepository, times(1)).findByUploaderId(1L);
    }

    @Test
    void deleteResource_removesFromDB() throws IOException {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));
        Path filePath = tempDir.resolve("test.pdf");
        Files.createFile(filePath);

        resourceService.deleteResource(1L, teacherUser);

        verify(resourceRepository, times(1)).delete(sampleResource);
        assertThat(Files.exists(filePath)).isFalse();
    }

    @Test
    void deleteResource_throwsAccessDenied_forUnauthorizedUser() {
        User student = new User();
        student.setId(2L);
        student.setUsername("student1");

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));

        assertThatThrownBy(() -> resourceService.deleteResource(1L, student))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void deleteResource_throwsEntityNotFound_whenMissing() {
        when(resourceRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.deleteResource(5L, teacherUser))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Resource not found");
    }
}
