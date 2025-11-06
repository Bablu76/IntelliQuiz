package com.intelliquiz.backend.controller;

import com.intelliquiz.backend.model.Resource;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.payload.response.MessageResponse;
import com.intelliquiz.backend.repository.UserRepository;
import com.intelliquiz.backend.service.PdfService;
import com.intelliquiz.backend.service.ResourceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 📄 ResourceController
 * Handles resource uploads (PDFs), text extraction, listing, and deletion.
 * Integrated with Apache PDFBox for PDF text extraction.
 */
@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Slf4j
public class ResourceController {

    private final ResourceService resourceService;
    private final PdfService pdfService;
    private final UserRepository userRepository;

    // ---------------------- 1️⃣ Upload PDF with text extraction ----------------------
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    public ResponseEntity<?> uploadResource(
            @RequestParam("file") MultipartFile file,
            @RequestParam("topic") String topic,
            Principal principal) {

        try {
            User uploader = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            log.info("📤 Upload initiated by {} for topic {}", uploader.getUsername(), topic);

            // ✅ Step 1: Extract text using PDFBox
            String extractedText = pdfService.extractText(file);
            log.info("📄 Extracted {} characters from PDF '{}'", extractedText.length(), file.getOriginalFilename());

            // ✅ Step 2: Save file + extracted text
            Resource resource = resourceService.saveResourceWithContext(file, uploader, topic, extractedText);
            log.info("✅ Resource saved: {} by {}", resource.getFileName(), uploader.getUsername());

            return ResponseEntity.ok(Map.of(
                    "message", "File uploaded and text extracted successfully",
                    "resourceId", resource.getId(),
                    "topic", topic,
                    "textLength", extractedText.length()
            ));

        } catch (Exception e) {
            log.error("❌ Failed to upload or extract text from PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Upload failed", "details", e.getMessage()));
        }
    }

    // ---------------------- 2️⃣ On-demand text extraction ----------------------
    @PostMapping("/extract")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    public ResponseEntity<?> extractPdfText(@RequestParam("file") MultipartFile file) {
        try {
            String extractedText = pdfService.extractText(file);
            return ResponseEntity.ok(Map.of(
                    "filename", file.getOriginalFilename(),
                    "length", extractedText.length(),
                    "pdfContext", extractedText
            ));
        } catch (Exception e) {
            log.error("❌ Error extracting PDF text: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to extract PDF content", "details", e.getMessage()));
        }
    }

    // ---------------------- 3️⃣ List current user's resources ----------------------
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    public ResponseEntity<?> listUserResources(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        List<Resource> resources = resourceService.getUserResources(user.getId());
        log.info("📋 Listing {} resources for {}", resources.size(), user.getUsername());
        return ResponseEntity.ok(resources);
    }

    // ---------------------- 4️⃣ Admin: list all resources ----------------------
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listAllResources() {
        List<Resource> all = resourceService.getAllResources();
        log.info("📂 Admin viewed {} resources", all.size());
        return ResponseEntity.ok(all);
    }

    // ---------------------- 5️⃣ Delete a resource ----------------------
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
