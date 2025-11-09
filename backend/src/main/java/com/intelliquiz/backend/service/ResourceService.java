package com.intelliquiz.backend.service;

import com.intelliquiz.backend.model.Resource;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.repository.ResourceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;

/**
 * 📘 ResourceService — Handles file persistence, retrieval, and extracted text management.
 *
 * 🧠 Used by ResourceController & QuizController for:
 *  - Uploading learning materials (with context for AI quiz generation)
 *  - Retrieving resource data during quiz creation
 *  - Safely deleting uploaded files (owner/admin only)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private static final Path UPLOAD_DIR = Paths.get("uploads").toAbsolutePath().normalize();

    // -------------------------------------------------------------------------
    // 🧩 1️⃣ Save File + Extracted Text (for AI Context)
    // -------------------------------------------------------------------------
    public Resource saveResourceWithContext(MultipartFile file, User uploader, String topic, String extractedText) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty.");
        }

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path safeFileName = Paths.get(Objects.requireNonNull(file.getOriginalFilename())).getFileName();
            Path filePath = UPLOAD_DIR.resolve(safeFileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("💾 File physically stored at '{}'", filePath);

            Resource resource = new Resource();
            resource.setTopic(topic);
            resource.setUploader(uploader);
            resource.setFileName(safeFileName.toString());
            resource.setFileType(file.getContentType());
            resource.setFileSize(file.getSize());
            resource.setExtractedText(extractedText);

            Resource saved = resourceRepository.save(resource);
            log.info("✅ Saved resource '{}' | topic='{}' | chars={} | user='{}'",
                    file.getOriginalFilename(),
                    topic,
                    extractedText != null ? extractedText.length() : 0,
                    uploader.getUsername());

            return saved;

        } catch (IOException e) {
            log.error("❌ Failed to store file '{}': {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("File storage error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 🧩 2️⃣ Save Resource Without Extracted Context (Legacy)
    // -------------------------------------------------------------------------
    public Resource saveResource(MultipartFile file, User uploader, String topic) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty.");
        }

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path safeFileName = Paths.get(  Objects.requireNonNull(file.getOriginalFilename())).getFileName();
            Path filePath = UPLOAD_DIR.resolve(safeFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Resource resource = new Resource();
            resource.setTopic(topic);
            resource.setUploader(uploader);
            resource.setFileName(safeFileName.toString());
            resource.setFileType(file.getContentType());
            resource.setFileSize(file.getSize());

            Resource saved = resourceRepository.save(resource);
            log.info("💾 Legacy resource saved '{}' | topic='{}' | by {}", file.getOriginalFilename(), topic, uploader.getUsername());
            return saved;

        } catch (IOException e) {
            log.error("❌ Failed to store file '{}': {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("File storage error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 🧩 3️⃣ Fetch Single Resource by ID (for QuizController)
    // -------------------------------------------------------------------------
    public Resource getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Resource not found for id=" + id));

        log.info("📘 Resource fetched | id={} | topic='{}' | file='{}' | chars={}",
                id, resource.getTopic(), resource.getFileName(),
                resource.getExtractedText() != null ? resource.getExtractedText().length() : 0);

        return resource;
    }

    // -------------------------------------------------------------------------
    // 🧩 4️⃣ Fetch All Resources Uploaded by a User
    // -------------------------------------------------------------------------
    public List<Resource> getUserResources(Long userId) {
        List<Resource> resources = resourceRepository.findByUploaderId(userId);
        log.info("📚 {} resources fetched for userId={}", resources.size(), userId);
        return resources;
    }

    // -------------------------------------------------------------------------
    // 🧩 5️⃣ Fetch All Resources (Admin Dashboard)
    // -------------------------------------------------------------------------
    public List<Resource> getAllResources() {
        List<Resource> all = resourceRepository.findAll();
        log.info("📂 Admin fetched {} total resources", all.size());
        return all;
    }

    // -------------------------------------------------------------------------
    // 🧩 6️⃣ Safe Deletion (Owner or Admin)
    // -------------------------------------------------------------------------
    public void deleteResource(Long id, User requester) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Resource not found with id=" + id));

        boolean isOwner = resource.getUploader().getId().equals(requester.getId());
        boolean isAdmin = requester.getRoles().toString().contains("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new SecurityException("You are not allowed to delete this resource.");
        }

        resourceRepository.delete(resource);
        log.warn("🗑️ Resource '{}' (topic='{}') deleted by {}", resource.getFileName(), resource.getTopic(), requester.getUsername());

        // Optional: Clean up physical file
        try {
            Path filePath = UPLOAD_DIR.resolve(resource.getFileName()).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("🧹 Deleted physical file '{}'", filePath);
            } else {
                log.warn("⚠️ Physical file not found for '{}'", resource.getFileName());
            }
        } catch (IOException e) {
            log.warn("⚠️ Failed to delete file '{}': {}", resource.getFileName(), e.getMessage());
        }
    }
}
