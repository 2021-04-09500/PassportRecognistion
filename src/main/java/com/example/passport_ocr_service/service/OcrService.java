package com.example.passport_ocr_service.service;

import com.example.passport_ocr_service.model.PassportData;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
public class OcrService {

    public String performOcr(MultipartFile file, String lang) throws Exception {
        ITesseract tesseract = new Tesseract();

        // IMPORTANT: this must point to the folder that CONTAINS tessdata
        // NOT tessdata itself
        tesseract.setDatapath("/usr/share/tesseract-ocr");

        tesseract.setLanguage(lang);

        File tempFile = File.createTempFile("ocr-", ".png");
        file.transferTo(tempFile);

        try {
            return tesseract.doOCR(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    public PassportData parseMrz(String ocrText) {
        return new PassportData(); // keep it simple for now
    }
}
