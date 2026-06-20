package com.siladocs.application.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FileAnalysisService.class);
    private static final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    public FileAnalysisResult analyzeFile(MultipartFile file, String courseCode) {
        try {
            String text = extractText(file);
            List<String> foundCodes = extractCourseCodesFromText(text);
            String matchedCode = findBestMatch(courseCode, foundCodes);
            double matchConfidence = calculateConfidence(courseCode, matchedCode);

            log.info("[FILE ANALYSIS] File: {}, Course: {}, MatchedCode: {}, Confidence: {}",
                    file.getOriginalFilename(), courseCode, matchedCode, matchConfidence);

            return new FileAnalysisResult(
                    courseCode,
                    matchedCode,
                    matchConfidence,
                    foundCodes,
                    matchConfidence >= 0.85
            );
        } catch (Exception e) {
            log.error("[FILE ANALYSIS] Error analyzing file {}: {}", file.getOriginalFilename(), e.getMessage());
            return new FileAnalysisResult(courseCode, null, 0.0, new ArrayList<>(), false);
        }
    }

    private String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();

        if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
            return extractTextFromPdf(file);
        } else if (filename != null && (filename.toLowerCase().endsWith(".docx") ||
                   filename.toLowerCase().endsWith(".doc"))) {
            return extractTextFromDocx(file);
        }

        return filename != null ? filename : "";
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        StringBuilder text = new StringBuilder();
        byte[] fileBytes = file.getBytes();
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            text.append(stripper.getText(document));
        }
        return text.toString();
    }

    private String extractTextFromDocx(MultipartFile file) throws IOException {
        StringBuilder text = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
        }
        return text.toString();
    }

    private List<String> extractCourseCodesFromText(String text) {
        List<String> codes = new ArrayList<>();

        // Pattern: 3-6 letters followed by 2-4 digits (e.g., MAT101, CS4012, BIO102)
        Pattern pattern = Pattern.compile("\\b([A-Z]{2,6}\\d{2,4})\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String code = matcher.group(1).toUpperCase();
            if (!codes.contains(code)) {
                codes.add(code);
            }
        }

        // Also extract from filename
        String[] parts = text.split("[/_\\-\\.]");
        for (String part : parts) {
            if (part.matches("^[A-Z]{2,6}\\d{2,4}$")) {
                String code = part.toUpperCase();
                if (!codes.contains(code)) {
                    codes.add(code);
                }
            }
        }

        log.info("[FILE ANALYSIS] Extracted codes: {}", codes);
        return codes;
    }

    private String findBestMatch(String courseCode, List<String> foundCodes) {
        if (foundCodes.isEmpty()) {
            return null;
        }

        String bestMatch = foundCodes.get(0);
        double bestScore = similarity.apply(courseCode.toUpperCase(), bestMatch.toUpperCase());

        for (String code : foundCodes) {
            double score = similarity.apply(courseCode.toUpperCase(), code.toUpperCase());
            if (score > bestScore) {
                bestScore = score;
                bestMatch = code;
            }
        }

        return bestMatch;
    }

    private double calculateConfidence(String courseCode, String matchedCode) {
        if (matchedCode == null) {
            return 0.0;
        }
        return similarity.apply(courseCode.toUpperCase(), matchedCode.toUpperCase());
    }

    public static class FileAnalysisResult {
        public final String courseCode;
        public final String detectedCode;
        public final double confidence;
        public final List<String> allDetectedCodes;
        public final boolean isMatch;

        public FileAnalysisResult(String courseCode, String detectedCode, double confidence,
                                List<String> allDetectedCodes, boolean isMatch) {
            this.courseCode = courseCode;
            this.detectedCode = detectedCode;
            this.confidence = confidence;
            this.allDetectedCodes = allDetectedCodes;
            this.isMatch = isMatch;
        }
    }
}
