package com.example.passport_ocr_service.controllers;

import com.example.passport_ocr_service.model.PassportData;
import com.example.passport_ocr_service.service.MrzParserService;
import com.example.passport_ocr_service.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(origins = "*") // Allow all origins for testing
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private MrzParserService mrzParser;

    /**
     * Accepts a real image file (jpg/png) and returns parsed passport data.
     * The frontend should send a FormData with key "image".
     */
    @PostMapping("/passport")
    public PassportData upload(@RequestParam("image") MultipartFile file) throws Exception {

        // Extract text using Tesseract
        String ocrText = ocrService.extractText(file);

        // Parse MRZ or passport info
        PassportData data = mrzParser.parse(ocrText);

        return data;
    }
}
