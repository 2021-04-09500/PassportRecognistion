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

        // Set the path to your local Tesseract installation
        // Point to the 'tessdata' folder inside your Tesseract path
        tesseract.setDatapath("C:/Users/lenovo/Downloads/tesseract-ocr-tesseract-9c516f4/tessdata");
        tesseract.setLanguage("eng");

        // Create a temporary file to store the uploaded image
        File tempFile = File.createTempFile("ocr-", ".png");
        file.transferTo(tempFile);

        try {
            // Perform OCR
            return tesseract.doOCR(tempFile);
        } finally {
            // Clean up temp file
            tempFile.delete();
        }
    }
}
