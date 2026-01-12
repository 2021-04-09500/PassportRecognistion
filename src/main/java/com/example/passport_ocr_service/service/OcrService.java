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
     * Perform OCR on uploaded image using Tesseract.
     */
    public String performOcr(MultipartFile file, String lang) throws Exception {
        ITesseract tesseract = new Tesseract();

        tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
        tesseract.setVariable("user_defined_dpi", "300"); // Improve accuracy
        tesseract.setPageSegMode(6); // Assume single uniform block
        tesseract.setOcrEngineMode(1); // LSTM engine
        tesseract.setLanguage(lang);

        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf(".")))
                .orElse(".png");

        File tempFile = File.createTempFile("ocr-", ext);
        file.transferTo(tempFile);

        try {
            String result = tesseract.doOCR(tempFile);
            System.out.println("Raw OCR result: " + result);
            return result;
        } finally {
            tempFile.delete();
        }
    }

    /**
     * Parse passport data from OCR text.
     * Uses MRZ first, then heuristic extraction if MRZ fails.
     */
    public PassportData extractPassportData(String ocrText) {
        PassportData data = new PassportData();
        if (ocrText == null || ocrText.trim().isEmpty()) return data;

        // Normalize text
        String normalized = ocrText.replaceAll("[\r\n]+", "\n");

        // --- 1. Try MRZ parsing first ---
        PassportData mrzData = parseMrz(normalized);
        if (mrzData.getPassportNumber() != null && !mrzData.getPassportNumber().isEmpty()) {
            return mrzData; // MRZ succeeded
        }

        // --- 2. Fallback: keyword + regex extraction ---
        // First Name / Last Name
        Pattern namePattern = Pattern.compile("(?i)(?:Surname|Last Name)[:\\s]+([A-Z\\s]+)");
        Matcher nameMatcher = namePattern.matcher(normalized);
        if (nameMatcher.find()) data.setLastName(nameMatcher.group(1).trim());

        namePattern = Pattern.compile("(?i)(?:Given Name|First Name)[:\\s]+([A-Z\\s]+)");
        nameMatcher = namePattern.matcher(normalized);
        if (nameMatcher.find()) data.setFirstName(nameMatcher.group(1).trim());

        // Passport Number
        Pattern passportPattern = Pattern.compile("(?i)(?:Passport No|Passport Number)[:\\s]*([A-Z0-9]{6,9})");
        Matcher passportMatcher = passportPattern.matcher(normalized);
        if (passportMatcher.find()) data.setPassportNumber(passportMatcher.group(1).trim());

        // Date of Birth
        Pattern dobPattern = Pattern.compile("\\b(\\d{2}\\s[A-Z][a-z]{2}\\s\\d{4})\\b"); // e.g., 07 Aug 1999
        Matcher dobMatcher = dobPattern.matcher(normalized);
        if (dobMatcher.find()) data.setDateOfBirth(formatDate(dobMatcher.group(1)));

        // Expiry Date
        Pattern expPattern = Pattern.compile("(?i)(?:Expiry Date|Date of Expiry)[:\\s]*(\\d{2}\\s[A-Z][a-z]{2}\\s\\d{4})");
        Matcher expMatcher = expPattern.matcher(normalized);
        if (expMatcher.find()) data.setExpiryDate(formatDate(expMatcher.group(1)));

        // Nationality
        Pattern natPattern = Pattern.compile("(?i)Nationality[:\\s]*([A-Z]{3})");
        Matcher natMatcher = natPattern.matcher(normalized);
        if (natMatcher.find()) data.setNationality(natMatcher.group(1).trim());

        // Gender
        Pattern genderPattern = Pattern.compile("\\b(Male|Female|M|F)\\b", Pattern.CASE_INSENSITIVE);
        Matcher genderMatcher = genderPattern.matcher(normalized);
        if (genderMatcher.find()) {
            String g = genderMatcher.group(1).toUpperCase();
            data.setGender(g.equals("M") ? "Male" : g.equals("F") ? "Female" : g);
        }

        return data;
    }

    /**
     * MRZ parsing: more tolerant version
     */
    public PassportData parseMrz(String ocrText) {
        PassportData data = new PassportData();

        // Remove newlines, extra spaces
        String normalized = ocrText.replaceAll("[\\s\r\n]+", "").toUpperCase();

        // Find long sequences of MRZ-like chars
        Pattern mrzCandidatePattern = Pattern.compile("[A-Z0-9<]{30,}");
        Matcher matcher = mrzCandidatePattern.matcher(normalized);
        StringBuilder mrzCandidate = new StringBuilder();
        while (matcher.find()) mrzCandidate.append(matcher.group());

        if (mrzCandidate.length() == 0) return data;

        // Flexible MRZ pattern
        Pattern mrzPattern = Pattern.compile(
                "([A-Z<]+)<<([A-Z<]+).{0,50}?(\\w{6,9})([A-Z]{3})(\\d{6})([MF<]).{0,50}?(\\d{6})"
        );
        Matcher mrzMatcher = mrzPattern.matcher(mrzCandidate.toString());
        if (mrzMatcher.find()) {
            data.setLastName(mrzMatcher.group(1).replace("<", " ").trim());
            data.setFirstName(mrzMatcher.group(2).replace("<", " ").trim());
            data.setPassportNumber(mrzMatcher.group(3).trim());
            data.setNationality(mrzMatcher.group(4).trim());
            data.setDateOfBirth(formatDateYYMMDD(mrzMatcher.group(5)));
            data.setGender("M".equals(mrzMatcher.group(6)) ? "Male" : "Female");
            if (mrzMatcher.groupCount() >= 7) data.setExpiryDate(formatDateYYMMDD(mrzMatcher.group(7)));
        }

        return data;
    }

    // --- Helpers ---

    public String formatDateYYMMDD(String yymmdd) {
        if (yymmdd == null || yymmdd.length() != 6) return "";
        return "20" + yymmdd.substring(0, 2) + "-" + yymmdd.substring(2, 4) + "-" + yymmdd.substring(4, 6);
    }

    public String formatDate(String dateStr) {
        // Converts "07 Aug 1999" -> "1999-08-07"
        try {
            String[] parts = dateStr.split(" ");
            if (parts.length != 3) return dateStr;
            String day = parts[0];
            String month = switch (parts[1].toLowerCase()) {
                case "jan" -> "01";
                case "feb" -> "02";
                case "mar" -> "03";
                case "apr" -> "04";
                case "may" -> "05";
                case "jun" -> "06";
                case "jul" -> "07";
                case "aug" -> "08";
                case "sep" -> "09";
                case "oct" -> "10";
                case "nov" -> "11";
                case "dec" -> "12";
                default -> "01";
            };
            return parts[2] + "-" + month + "-" + day;
        } catch (Exception e) {
            return dateStr;
        }
    }
}
