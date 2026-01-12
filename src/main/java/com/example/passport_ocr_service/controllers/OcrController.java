package com.example.passport_ocr_service.controllers;

import com.example.passport_ocr_service.model.PassportData;
import com.example.passport_ocr_service.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @PostMapping("/passport")
    public ResponseEntity<?> processPassport(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "lang", defaultValue = "eng") String lang
    ) {
        try {
            // Perform OCR
            String ocrText = ocrService.performOcr(image, lang);
            System.out.println("Raw OCR text sent to frontend: " + ocrText);
            // Parse MRZ
            PassportData passportData = ocrService.parseMrz(ocrText);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("rawText", ocrText);
            response.put("firstName", passportData.getFirstName());
            response.put("lastName", passportData.getLastName());
            response.put("passportNumber", passportData.getPassportNumber());
            response.put("nationality", passportData.getNationality());
            response.put("dateOfBirth", passportData.getDateOfBirth());
            response.put("gender", passportData.getGender());
            response.put("expiryDate", passportData.getExpiryDate());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
