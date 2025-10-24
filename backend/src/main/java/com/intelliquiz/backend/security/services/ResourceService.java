package com.intelliquiz.backend.security.services;

import com.intelliquiz.backend.exception.BadRequestException;
import com.intelliquiz.backend.model.Resource;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.repository.ResourceRepository;
import com.intelliquiz.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Transactional
    public Resource saveResource(MultipartFile file, User uploader, String topic) {
        log.info("📤 Upload initiated by {} for topic {}", uploader.getUsername(), topic);

        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new BadRequestException("File cannot be empty");
        }

        String originalName = file.getOriginalFilename();
        String contentType = file.getContentType();

        if (!originalName.toLowerCase().endsWith(".pdf") &&
                (contentType == null || !contentType.equalsIgnoreCase("application/pdf"))) {
            log.warn("❌ Invalid file upload attempt by {} : {}", uploader.getUsername(), originalName);
            throw new BadRequestException("Only PDF files are allowed");
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("📁 Created uploads directory at {}", uploadPath.toAbsolutePath());
            }

            String uniqueFileName = System.currentTimeMillis() + "_" + originalName.replaceAll("\\s+", "_");
            Path storagePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), storagePath, StandardCopyOption.REPLACE_EXISTING);

            Resource resource = new Resource();
            resource.setFileName(uniqueFileName);
            resource.setFileType("application/pdf");
            resource.setTopic(topic);
            resource.setUploader(uploader);
            resource.setUploaderRole(
                    uploader.getRoles().iterator().next().getName().name());
            resource.setUploadedAt(LocalDateTime.now());

            Resource saved = resourceRepository.save(resource);
            log.info("✅ Saved resource {} by {}", uniqueFileName, uploader.getUsername());
            return saved;

        } catch (IOException e) {
            log.error("❌ Resource upload failed for {} : {}", uploader.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Error saving file: " + e.getMessage());
        }
    }

    public List<Resource> getUserResources(Long userId) {
        List<Resource> list = resourceRepository.findByUploaderId(userId);
        log.info("📋 Retrieved {} resources for user ID {}", list.size(), userId);
        return list;
    }

    public List<Resource> getAllResources() {
        List<Resource> all = resourceRepository.findAll();
        log.info("📂 Admin accessed all resources: count = {}", all.size());
        return all;
    }

    @Transactional
    public void deleteResource(Long id, User currentUser) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Resource not found with ID: " + id));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"));

        if (!isAdmin && !resource.getUploader().getId().equals(currentUser.getId())) {
            log.warn("⚠️ Unauthorized delete attempt by {} on resource {}", currentUser.getUsername(), id);
            throw new AccessDeniedException("Access denied: Cannot delete another user's file");
        }

        try {
            Path filePath = Paths.get(uploadDir, resource.getFileName());
            Files.deleteIfExists(filePath);
            log.warn("🗑️ Resource file {} deleted from disk", resource.getFileName());
        } catch (IOException e) {
            log.error("⚠️ Failed to delete file {}: {}", resource.getFileName(), e.getMessage());
        }

        resourceRepository.delete(resource);
        log.info("✅ Resource {} deleted by {}", id, currentUser.getUsername());
    }
}
