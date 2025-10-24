package com.intelliquiz.backend.controller;

import com.intelliquiz.backend.model.Resource;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.payload.response.MessageResponse;
import com.intelliquiz.backend.repository.UserRepository;
import com.intelliquiz.backend.security.services.ResourceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Slf4j
public class ResourceController {

    private final ResourceService resourceService;
    private final UserRepository userRepository;

    /**
     * Upload a new resource (PDF).
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    public ResponseEntity<?> uploadResource(@RequestParam("file") MultipartFile file,
                                            @RequestParam("topic") String topic,
                                            Principal principal) {
        User uploader = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        log.info("📤 Upload initiated by {} for topic {}", uploader.getUsername(), topic);
        Resource resource = resourceService.saveResource(file, uploader, topic);
        log.info("✅ Upload successful: {} by {}", resource.getFileName(), uploader.getUsername());

        return ResponseEntity.ok(new MessageResponse("File uploaded successfully"));
    }

    /**
     * List resources for the current user.
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    public ResponseEntity<?> listUserResources(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<Resource> resources = resourceService.getUserResources(user.getId());
        log.info("📋 Listing {} resources for {}", resources.size(), user.getUsername());
        return ResponseEntity.ok(resources);
    }

    /**
     * Admin: list all resources.
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listAllResources() {
        List<Resource> all = resourceService.getAllResources();
        log.info("📂 Admin viewed {} resources", all.size());
        return ResponseEntity.ok(all);
    }

    /**
     * Delete a resource (own or admin privilege).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','ADMIN')")
    public ResponseEntity<?> deleteResource(@PathVariable Long id, Principal principal) {
        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        resourceService.deleteResource(id, currentUser);
        log.warn("🗑️ Resource {} deleted by {}", id, currentUser.getUsername());

        return ResponseEntity.ok(new MessageResponse("Resource deleted successfully"));
    }
}
