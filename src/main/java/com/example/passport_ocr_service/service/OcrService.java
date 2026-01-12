package com.example.passport_ocr_service.service;

import com.example.passport_ocr_service.model.PassportData;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    /**
     * Perform OCR on uploaded image using specified language(s).
     * @param file Multipart image file
     * @param lang Language code(s), e.g., "eng" or "eng+fra+deu"
     * @return OCR text
     * @throws Exception
     */
    public String performOcr(MultipartFile file, String lang) throws Exception {
        ITesseract tesseract = new Tesseract();

        // Use system-installed Tesseract
        tesseract.setDatapath("/usr/share/tesseract-ocr");

        // Set language(s)
        tesseract.setLanguage(lang);

        // Save uploaded file to temp location
        File tempFile = File.createTempFile("ocr-", ".jpg");
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

        // MRZ pattern for 2-line passports (44 chars per line)
        Pattern pattern = Pattern.compile(
                "P<([A-Z<]+)<<([A-Z<]+)" + // Last name + first name
                        ".+?(\\w{9})" +     // Passport number
                        "([A-Z]{3})" +      // Nationality
                        ".+?(\\d{6})" +     // Date of birth YYMMDD
                        "([MF<])"           // Gender
        );

        Matcher matcher = pattern.matcher(normalized);
        if (matcher.find()) {
            data.setLastName(matcher.group(1).replace("<", " ").trim());
            data.setFirstName(matcher.group(2).replace("<", " ").trim());
            data.setPassportNumber(matcher.group(3).trim());
            data.setNationality(matcher.group(4).trim());
            data.setDateOfBirth(formatDate(matcher.group(5)));
            data.setGender(matcher.group(6).equals("M") ? "Male" : "Female");
        }

        return data;
    }

    private String formatDate(String yymmdd) {
        if (yymmdd == null || yymmdd.length() != 6) return "";
        String year = "20" + yymmdd.substring(0, 2);
        String month = yymmdd.substring(2, 4);
        String day = yymmdd.substring(4, 6);
        return year + "-" + month + "-" + day;
    }
}
