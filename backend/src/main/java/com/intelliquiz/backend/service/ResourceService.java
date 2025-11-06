package com.intelliquiz.backend.service;

import com.intelliquiz.backend.model.Resource;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.repository.ResourceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;

    // ✅ Save file + extracted text (for LLM)
    public Resource saveResourceWithContext(MultipartFile file, User uploader, String topic, String extractedText) {
        Resource resource = new Resource();
        resource.setTopic(topic);
        resource.setUploader(uploader);
        resource.setFileName(file.getOriginalFilename());
        resource.setFileType(file.getContentType());
        resource.setFileSize(file.getSize()); // ✅ now works
        resource.setExtractedText(extractedText);
        log.info("💾 Saving resource '{}' ({}) by {}", file.getOriginalFilename(), topic, uploader.getUsername());
        return resourceRepository.save(resource);
    }

    // ✅ Legacy save (no text extraction)
    public Resource saveResource(MultipartFile file, User uploader, String topic) {
        Resource resource = new Resource();
        resource.setTopic(topic);
        resource.setUploader(uploader);
        resource.setFileName(file.getOriginalFilename());
        resource.setFileType(file.getContentType());
        resource.setFileSize(file.getSize());
        log.info("💾 Saving legacy resource '{}' ({}) by {}", file.getOriginalFilename(), topic, uploader.getUsername());
        return resourceRepository.save(resource);
    }

    // ✅ Fetch resources by user
    public List<Resource> getUserResources(Long userId) {
        return resourceRepository.findByUploaderId(userId);
    }

    // ✅ Fetch all (for admin)
    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    // ✅ Delete a resource safely
    public void deleteResource(Long id, User requester) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Resource not found"));

        boolean isOwner = resource.getUploader().getId().equals(requester.getId());
        boolean isAdmin = requester.getRoles().toString().contains("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new SecurityException("You are not allowed to delete this resource.");
        }

        resourceRepository.delete(resource);
        log.warn("🗑️ Resource '{}' deleted by {}", resource.getFileName(), requester.getUsername());
    }
}
