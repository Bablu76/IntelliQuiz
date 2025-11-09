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
 * 📘 PdfService — extracts text content safely from uploaded PDFs
 */
@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    public String extractText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Empty or invalid PDF file.");
        }

        try (InputStream input = file.getInputStream();
             PDDocument doc = PDDocument.load(input)) {

            String text = new PDFTextStripper().getText(doc).trim();
            if (text.isBlank()) throw new IOException("No readable text found (possibly scanned PDF).");

            log.info("✅ Extracted {} characters from '{}'", text.length(), file.getOriginalFilename());
            return text;
        }
    }
}
