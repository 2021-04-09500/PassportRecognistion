package com.example.passport_ocr_service.service;

import com.example.passport_ocr_service.model.PassportData;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    /**
     * Perform OCR on an uploaded file using Tesseract.
     * Supports all installed languages.
     */
    public String performOcr(MultipartFile file, String language) throws Exception {
        ITesseract tesseract = new Tesseract();

        // Use system tessdata installed via apt
        tesseract.setDatapath(System.getenv("TESSDATA_PREFIX")); // /usr/share/tesseract-ocr/4.00/tessdata/
        tesseract.setLanguage(language); // e.g., "eng", "fra", "deu" or multiple: "eng+fra+deu"

        // Save MultipartFile to temp
        File tempFile = File.createTempFile("ocr-", ".jpg");
        file.transferTo(tempFile);

        try {
            return tesseract.doOCR(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    /**
     * Parse MRZ (Machine Readable Zone) from passport OCR text
     */
    public PassportData parseMrz(String ocrText) {
        PassportData data = new PassportData();

        Pattern pattern = Pattern.compile(
                "P<([A-Z<]+)<<([A-Z<]+)[\\s\\S]+?(\\d{9})[A-Z0-9]<([A-Z]{3})[\\s\\S]+?(\\d{6})[MF<](\\d{6})"
        );
        Matcher matcher = pattern.matcher(ocrText.replaceAll("\\s+", ""));
        if (matcher.find()) {
            data.setLastName(matcher.group(1).replace("<", " ").trim());
            data.setFirstName(matcher.group(2).replace("<", " ").trim());
            data.setPassportNumber(matcher.group(3).trim());
            data.setNationality(matcher.group(4).trim());
            data.setDateOfBirth(formatDate(matcher.group(5)));
            data.setGender(matcher.group(6).startsWith("M") ? "Male" : "Female");
        }
        return data;
    }

    private String formatDate(String yymmdd) {
        if (yymmdd.length() != 6) return "";
        String year = yymmdd.substring(0, 2);
        String month = yymmdd.substring(2, 4);
        String day = yymmdd.substring(4, 6);
        return "20" + year + "-" + month + "-" + day;
    }
}
