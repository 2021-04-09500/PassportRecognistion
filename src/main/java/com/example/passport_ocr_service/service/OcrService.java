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

        // Point to tessdata inside your resources/static folder
        // Note: Tesseract expects the parent folder of tessdata
        String tessDataPath = new File("src/main/resources/static/tesseract").getAbsolutePath();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("eng"); // use eng.traineddata

        // Save the uploaded file temporarily
        File tempFile = File.createTempFile("ocr-", ".png");
        file.transferTo(tempFile);

        try {
            return tesseract.doOCR(tempFile);
        } finally {
            tempFile.delete();
        }
    }
}
