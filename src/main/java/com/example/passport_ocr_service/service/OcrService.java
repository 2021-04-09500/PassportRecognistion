package com.example.passport_ocr_service.service;

import com.example.passport_ocr_service.model.PassportData;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    // -----------------------------
    // Copy tessdata to temp folder
    // -----------------------------
    private File prepareTessData() throws IOException {
        ClassPathResource tessResource = new ClassPathResource("tessdata/eng.traineddata");
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "tessdata");
        if (!tempDir.exists()) tempDir.mkdirs();

        File engFile = new File(tempDir, "eng.traineddata");
        if (!engFile.exists()) {
            try (InputStream is = tessResource.getInputStream();
                 FileOutputStream fos = new FileOutputStream(engFile)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }
        }
        return tempDir.getParentFile(); // Tess4J expects parent folder of tessdata
    }

    // -----------------------------
    // Perform OCR
    // -----------------------------
    public String performOcr(MultipartFile file) throws Exception {
        File tessDir = prepareTessData();

        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDir.getAbsolutePath());
        tesseract.setLanguage("eng");

        File tempFile = File.createTempFile("ocr-", ".jpg");
        file.transferTo(tempFile);

        try {
            return tesseract.doOCR(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    // -----------------------------
    // Parse MRZ from OCR text
    // -----------------------------
    public PassportData parseMrz(String ocrText) {
        PassportData data = new PassportData();

        // Simple MRZ pattern for passport (2 lines, 44 chars each)
        Pattern pattern = Pattern.compile("P<([A-Z<]+)<<([A-Z<]+)[\\s\\S]+?(\\d{9})[A-Z0-9]<([A-Z]{3})[\\s\\S]+?(\\d{6})[MF<](\\d{6})");
        Matcher matcher = pattern.matcher(ocrText.replaceAll("\\s+", ""));
        if (matcher.find()) {
            data.setLastName(matcher.group(1).replace("<", " ").trim());
            data.setFirstName(matcher.group(2).replace("<", " ").trim());
            data.setPassportNumber(matcher.group(3).trim());
            data.setNationality(matcher.group(4).trim());
            data.setDateOfBirth(formatDate(matcher.group(5)));
            data.setGender(matcher.group(6).startsWith("M") ? "M" : "F");
            // Expiry date extraction may require adjustment
        }
        return data;
    }

    private String formatDate(String yymmdd) {
        if (yymmdd.length() != 6) return "";
        String year = yymmdd.substring(0, 2);
        String month = yymmdd.substring(2, 4);
        String day = yymmdd.substring(4, 6);
        return "20" + year + "-" + month + "-" + day; // naive 2000+ assumption
    }
}
