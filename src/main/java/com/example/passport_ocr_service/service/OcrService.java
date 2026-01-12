package com.example.passport_ocr_service.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
public class OcrService {

    public String extractText(MultipartFile file) throws Exception {

        ITesseract tesseract = new Tesseract();

        // point to the folder that CONTAINS tessdata
        tesseract.setDatapath("tessdata");
        tesseract.setLanguage("eng");

        File tempFile = File.createTempFile("ocr-", ".png");
        file.transferTo(tempFile);

        try {
            return tesseract.doOCR(tempFile);
        } finally {
            tempFile.delete();
        }
    }
}
