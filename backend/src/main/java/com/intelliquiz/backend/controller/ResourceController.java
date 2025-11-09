package com.intelliquiz.backend.controller;

import com.intelliquiz.backend.model.Resource;
import com.intelliquiz.backend.model.User;
import com.intelliquiz.backend.payload.response.MessageResponse;
import com.intelliquiz.backend.repository.ResourceRepository;
import com.intelliquiz.backend.repository.UserRepository;
import com.intelliquiz.backend.service.PdfService;
import com.intelliquiz.backend.service.ResourceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 📄 ResourceController
 * Handles resource uploads (PDFs), extraction, listing, deletion & download.
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
    private final ResourceRepository resourceRepository;

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

            // ✅ Extract text using PDFBox
            String extractedText = pdfService.extractText(file);
            log.info("📄 Extracted {} characters from PDF '{}'", extractedText.length(), file.getOriginalFilename());

            // ✅ Save file + extracted text
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
    // ✅ Require authentication & return only logged-in user's resources
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listUserResources(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }

        String username = principal.getName();
        log.info("📥 GET /resources/list for user '{}'", username);

        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        var list = resourceService.getUserResources(user.getId());
        return ResponseEntity.ok(list);
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

    // ---------------------- 6️⃣ Download a stored file (for AI quiz / preview) ----------------------
    @GetMapping("/download/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(@PathVariable Long id) {
        com.intelliquiz.backend.model.Resource dbRes = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found in DB."));

        Path filePath = Paths.get("uploads").resolve(dbRes.getFileName()).normalize();

        try {
            if (!Files.exists(filePath)) {
                log.error("⚠️ File missing on disk for '{}'", dbRes.getFileName());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            org.springframework.core.io.UrlResource fileResource = new org.springframework.core.io.UrlResource(filePath.toUri());
            if (!fileResource.exists() || !fileResource.isReadable()) {
                log.error("⚠️ File unreadable: {}", filePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            log.info("📤 Downloading file '{}'", dbRes.getFileName());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + dbRes.getFileName() + "\"")
                    .body(fileResource);

        } catch (MalformedURLException e) {
            log.error("❌ Invalid file path for {}: {}", dbRes.getFileName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

}
