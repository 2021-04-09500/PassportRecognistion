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

        // Datapath must match Docker installation
        tesseract.setDatapath("/usr/share/tesseract-ocr/4.00");
        tesseract.setLanguage(lang);

        // Use correct extension for temp file
        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf(".")))
                .orElse(".png");

        File tempFile = File.createTempFile("ocr-", ext);
        file.transferTo(tempFile);

        try {
            return tesseract.doOCR(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    /**
     * Parse MRZ from passport OCR text.
     */
    public PassportData parseMrz(String ocrText) {
        PassportData data = new PassportData();
        if (ocrText == null) return data;

        String normalized = ocrText.replaceAll("\\s+", "");

        Pattern pattern = Pattern.compile(
                "P<([A-Z<]+)<<([A-Z<]+).+?(\\w{9})([A-Z]{3}).+?(\\d{6})([MF<])"
        );

        Matcher matcher = pattern.matcher(normalized);

        if (matcher.find()) {
            data.setLastName(matcher.group(1).replace("<", " ").trim());
            data.setFirstName(matcher.group(2).replace("<", " ").trim());
            data.setPassportNumber(matcher.group(3).trim());
            data.setNationality(matcher.group(4).trim());
            data.setDateOfBirth(formatDate(matcher.group(5)));
            data.setGender("M".equals(matcher.group(6)) ? "Male" : "Female");
        }

        return data;
    }

    private String formatDate(String yymmdd) {
        if (yymmdd == null || yymmdd.length() != 6) return "";
        return "20" + yymmdd.substring(0, 2) + "-" +
                yymmdd.substring(2, 4) + "-" +
                yymmdd.substring(4, 6);
    }
}
