package com.example.passport_ocr_service.service;

import com.example.passport_ocr_service.model.PassportData;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    /**
     * Perform OCR on uploaded image using specified language(s).
     */
    public String performOcr(MultipartFile file, String lang) throws Exception {
        ITesseract tesseract = new Tesseract();

        // Critical fixes for better accuracy and to suppress "Invalid resolution 0 dpi" warning
        tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
        tesseract.setVariable("user_defined_dpi", "300");      // ← Forces 300 DPI treatment
        tesseract.setPageSegMode(6);                           // Assume single uniform block (great for MRZ)
        tesseract.setOcrEngineMode(1);                         // LSTM engine

        tesseract.setLanguage(lang);

        // Use correct extension for temp file
        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf(".")))
                .orElse(".png");

        File tempFile = File.createTempFile("ocr-", ext);
        file.transferTo(tempFile);

        try {
            String result = tesseract.doOCR(tempFile);
            // Optional: Log raw OCR for debugging (check Railway logs)
            System.out.println("Raw OCR result: " + result);
            return result;
        } finally {
            tempFile.delete();
        }
    }

    /**
     * Parse MRZ from passport OCR text – more tolerant to noise and missing prefixes.
     */
    public PassportData parseMrz(String ocrText) {
        PassportData data = new PassportData();
        if (ocrText == null || ocrText.trim().isEmpty()) return data;

        // Normalize aggressively
        String normalized = ocrText.replaceAll("\\s+", "")
                .replaceAll("[cCkKxX]", "<")    // Misread <
                .replaceAll("[0O]", "0")        // O → 0
                .replaceAll("[lI]", "1")        // l/I → 1
                .replaceAll("[5S]", "5")        // S → 5 if needed
                .toUpperCase();

        // Find MRZ candidate substrings (long sequences of A-Z0-9<)
        Pattern candidatePattern = Pattern.compile("[A-Z0-9<]{30,}");
        Matcher candidateMatcher = candidatePattern.matcher(normalized);
        String mrzCandidate = "";
        while (candidateMatcher.find()) {
            mrzCandidate += candidateMatcher.group();  // Concat potential MRZ parts
        }

        if (mrzCandidate.isEmpty()) {
            System.out.println("No MRZ candidate found. Normalized: " + normalized);
            return data;
        }

        // Flexible pattern starting from surname (handles missing P<USA)
        Pattern pattern = Pattern.compile(
                "([A-Z<]+)<<([A-Z<]+).{0,50}?(\\w{9})([A-Z]{3})(\\d{6})([MF<]).{0,50}?(\\d{6})"
        );
        Matcher matcher = pattern.matcher(mrzCandidate);

        if (matcher.find()) {
            data.setLastName(matcher.group(1).replace("<", " ").trim());
            data.setFirstName(matcher.group(2).replace("<", " ").trim());
            data.setPassportNumber(matcher.group(3).trim());
            data.setNationality(matcher.group(4).trim());
            data.setDateOfBirth(formatDate(matcher.group(5)));
            data.setGender("M".equals(matcher.group(6)) ? "Male" : "Female");
            if (matcher.groupCount() >= 7) {
                data.setExpiryDate(formatDate(matcher.group(7)));
            }
            System.out.println("Parsed MRZ data: " + data);  // Debug
        } else {
            System.out.println("MRZ parsing failed. Candidate: " + mrzCandidate);
        }

        return data;
    }

    private String formatDate(String yymmdd) {
        if (yymmdd == null || yymmdd.length() != 6) return "";
        return "20" + yymmdd.substring(0, 2) + "-" + yymmdd.substring(2, 4) + "-" + yymmdd.substring(4, 6);
    }
}