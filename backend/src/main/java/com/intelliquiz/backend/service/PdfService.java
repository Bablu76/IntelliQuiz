package com.intelliquiz.backend.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 📘 PdfService
 * Extracts plain text from uploaded PDF files using Apache PDFBox.
 * ✅ Reads complete text — no truncation.
 * ✅ Handles text-only and scanned PDFs safely.
 */
@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    /**
     * Extracts all text from a PDF file.
     *
     * @param file uploaded PDF file
     * @return extracted text (complete)
     * @throws IOException if the file cannot be read or contains no text
     */
    public String extractText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Empty or invalid PDF file.");
        }

        try (InputStream input = file.getInputStream();
             PDDocument document = PDDocument.load(input)) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // read text in visual order
            String text = stripper.getText(document).trim();

            if (text.isBlank()) {
                throw new IOException("No readable text content found in PDF (possibly scanned).");
            }

            log.info("✅ Successfully extracted {} characters from PDF '{}'", text.length(), file.getOriginalFilename());
            return text;

        } catch (IOException e) {
            log.error("❌ I/O error while reading PDF '{}': {}", file.getOriginalFilename(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error while extracting PDF '{}': {}", file.getOriginalFilename(), e.getMessage());
            throw new IOException("Failed to extract text from PDF. Ensure it’s not scanned-only content.");
        }
    }
}
