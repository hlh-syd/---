package com.research.workbench.knowledge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeExtractionService {

    public String extractText(String storagePath, String fileExt, String fallbackText) {
        if (StringUtils.hasText(storagePath)) {
            try {
                Path path = Path.of(storagePath);
                if (Files.exists(path)) {
                    return extractFromPath(path, fileExt, fallbackText);
                }
            } catch (Exception ignored) {
            }
        }
        return fallbackText == null ? "" : fallbackText;
    }

    private String extractFromPath(Path path, String fileExt, String fallbackText) throws IOException {
        String ext = fileExt == null ? "" : fileExt.toLowerCase();
        return switch (ext) {
            case "txt", "md", "csv", "tsv", "json", "xml" -> Files.readString(path, StandardCharsets.UTF_8);
            case "docx" -> extractDocx(path);
            case "pdf" -> extractPdf(path);
            default -> Files.isReadable(path) ? Files.readString(path, StandardCharsets.UTF_8) : fallbackText;
        };
    }

    private String extractDocx(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path); XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder builder = new StringBuilder();
            document.getParagraphs().forEach(paragraph -> builder.append(paragraph.getText()).append('\n'));
            return builder.toString();
        }
    }

    private String extractPdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
