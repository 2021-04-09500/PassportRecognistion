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
     * Parse MRZ from passport OCR text – more tolerant version
     */
    public PassportData parseMrz(String ocrText) {
        PassportData data = new PassportData();
        if (ocrText == null || ocrText.trim().isEmpty()) {
            return data;
        }

        // Normalize aggressively to handle OCR noise
        String normalized = ocrText
                .replaceAll("\\s+", "")                // Remove all whitespace
                .replaceAll("[cCkKxX]", "<")           // Common misreads of <
                .replaceAll("[0O]", "0")               // O → 0
                .replaceAll("[lI]", "1")               // l/I → 1
                .toUpperCase();

        // Improved TD3/MRZ pattern – more flexible
        Pattern pattern = Pattern.compile(
                "P<([A-Z<]{3})<([A-Z<]+)<<([A-Z<]+).+?" +   // P<country<<surname<<givennames...
                        "(\\w{9})([A-Z]{3})" +                      // Passport number + nationality
                        "(\\d{6})([MF<])" +                         // DOB + sex
                        ".+?(\\d{6})"                               // Optional: expiry date
        );

        Matcher matcher = pattern.matcher(normalized);

        if (matcher.find()) {
            data.setLastName(matcher.group(3).replace("<", " ").trim());
            data.setFirstName(matcher.group(2).replace("<", " ").trim());
            data.setPassportNumber(matcher.group(4).trim());
            data.setNationality(matcher.group(5).trim());
            data.setDateOfBirth(formatDate(matcher.group(6)));
            data.setGender("M".equals(matcher.group(7)) ? "Male" : "Female");

            // If expiry is captured in group 8
            if (matcher.groupCount() >= 8) {
                data.setExpiryDate(formatDate(matcher.group(8)));
            }
        } else {
            // Debug log if parsing fails
            System.out.println("MRZ parsing failed. Raw normalized text: " + normalized);
        }

        return data;
    }

    private String formatDate(String yymmdd) {
        if (yymmdd == null || yymmdd.length() != 6) return "";
        return "20" + yymmdd.substring(0, 2) + "-" + yymmdd.substring(2, 4) + "-" + yymmdd.substring(4, 6);
    }
}