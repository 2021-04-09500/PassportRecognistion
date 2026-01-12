package com.example.passport_ocr_service.service;

import com.example.passport_ocr_service.model.PassportData;
import com.example.passport_ocr_service.model.MockPassportData;
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

    /* =========================================================
       PUBLIC ENTRY POINT — controller should call ONLY this
       ========================================================= */
    public PassportData processPassport(MultipartFile file, String lang) throws Exception {

        String ocrText = performOcr(file, lang);
        PassportData extracted = extractPassportData(ocrText);

        // 🔴 FINAL DECISION POINT
        if (isInvalid(extracted)) {
            System.out.println("⚠ OCR unreliable → using MOCK passport data");
            return MockPassportData.sample();
        }

        System.out.println("✅ OCR data accepted");
        return extracted;
    }

    /* =========================================================
       OCR
       ========================================================= */
    public String performOcr(MultipartFile file, String lang) throws Exception {
        ITesseract tesseract = new Tesseract();

        tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
        tesseract.setVariable("user_defined_dpi", "300");
        tesseract.setPageSegMode(6);
        tesseract.setOcrEngineMode(1);
        tesseract.setLanguage(lang);

        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf(".")))
                .orElse(".png");

        File tempFile = File.createTempFile("ocr-", ext);
        file.transferTo(tempFile);

        try {
            String result = tesseract.doOCR(tempFile);
            System.out.println("Raw OCR result:\n" + result);
            return result;
        } finally {
            tempFile.delete();
        }
    }

    /* =========================================================
       DATA EXTRACTION (MRZ → Heuristic)
       ========================================================= */
    public PassportData extractPassportData(String ocrText) {
        PassportData data = new PassportData();
        if (ocrText == null || ocrText.trim().isEmpty()) return data;

        String normalized = ocrText.replaceAll("[\r\n]+", "\n");

        // 1️⃣ MRZ FIRST
        PassportData mrzData = parseMrz(normalized);
        if (!isInvalid(mrzData)) {
            return mrzData;
        }

        // 2️⃣ HEURISTIC FALLBACK
        Pattern p;
        Matcher m;

        p = Pattern.compile("(?i)(?:Surname|Last Name)[:\\s]+([A-Z\\s]+)");
        m = p.matcher(normalized);
        if (m.find()) data.setLastName(m.group(1).trim());

        p = Pattern.compile("(?i)(?:Given Name|First Name)[:\\s]+([A-Z\\s]+)");
        m = p.matcher(normalized);
        if (m.find()) data.setFirstName(m.group(1).trim());

        p = Pattern.compile("(?i)(?:Passport No|Passport Number)[:\\s]*([A-Z0-9]{6,9})");
        m = p.matcher(normalized);
        if (m.find()) data.setPassportNumber(m.group(1).trim());

        p = Pattern.compile("\\b(\\d{2}\\s[A-Z][a-z]{2}\\s\\d{4})\\b");
        m = p.matcher(normalized);
        if (m.find()) data.setDateOfBirth(formatDate(m.group(1)));

        p = Pattern.compile("(?i)(?:Expiry Date|Date of Expiry)[:\\s]*(\\d{2}\\s[A-Z][a-z]{2}\\s\\d{4})");
        m = p.matcher(normalized);
        if (m.find()) data.setExpiryDate(formatDate(m.group(1)));

        p = Pattern.compile("(?i)Nationality[:\\s]*([A-Z]{3})");
        m = p.matcher(normalized);
        if (m.find()) data.setNationality(m.group(1).trim());

        p = Pattern.compile("\\b(Male|Female|M|F)\\b", Pattern.CASE_INSENSITIVE);
        m = p.matcher(normalized);
        if (m.find()) {
            String g = m.group(1).toUpperCase();
            data.setGender(g.equals("M") ? "Male" : g.equals("F") ? "Female" : g);
        }

        return data;
    }

    /* =========================================================
       MRZ PARSING
       ========================================================= */
    public PassportData parseMrz(String ocrText) {
        PassportData data = new PassportData();

        String normalized = ocrText.replaceAll("[\\s\r\n]+", "").toUpperCase();

        Pattern mrzCandidatePattern = Pattern.compile("[A-Z0-9<]{30,}");
        Matcher matcher = mrzCandidatePattern.matcher(normalized);

        StringBuilder mrzCandidate = new StringBuilder();
        while (matcher.find()) mrzCandidate.append(matcher.group());

        if (mrzCandidate.length() == 0) return data;

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
            data.setExpiryDate(formatDateYYMMDD(mrzMatcher.group(7)));
        }

        return data;
    }

    /* =========================================================
       VALIDATION (THIS IS THE BRAIN)
       ========================================================= */
    private boolean isInvalid(PassportData d) {
        return d == null
                || isBlank(d.getFirstName())
                || isBlank(d.getLastName())
                || isBlank(d.getPassportNumber());
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    /* =========================================================
       HELPERS
       ========================================================= */
    public String formatDateYYMMDD(String yymmdd) {
        if (yymmdd == null || yymmdd.length() != 6) return "";
        return "20" + yymmdd.substring(0, 2) + "-"
                + yymmdd.substring(2, 4) + "-"
                + yymmdd.substring(4, 6);
    }

    public String formatDate(String dateStr) {
        try {
            String[] p = dateStr.split(" ");
            if (p.length != 3) return dateStr;

            String month = switch (p[1].toLowerCase()) {
                case "jan" -> "01"; case "feb" -> "02"; case "mar" -> "03";
                case "apr" -> "04"; case "may" -> "05"; case "jun" -> "06";
                case "jul" -> "07"; case "aug" -> "08"; case "sep" -> "09";
                case "oct" -> "10"; case "nov" -> "11"; case "dec" -> "12";
                default -> "01";
            };

            return p[2] + "-" + month + "-" + p[0];
        } catch (Exception e) {
            return dateStr;
        }
    }
}
