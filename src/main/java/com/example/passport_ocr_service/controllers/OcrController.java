package com.example.passport_ocr_service.controllers;

import com.example.passport_ocr_service.model.PassportData;
import com.example.passport_ocr_service.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?> handlePassport(@RequestParam("image") MultipartFile file) {
        try {
            String rawText = ocrService.performOcr(file, "eng");
            PassportData mrzData = ocrService.parseMrz(rawText);

            Map<String, Object> response = new HashMap<>();
            response.put("rawText", rawText);
            response.put("firstName", mrzData.getFirstName());
            response.put("lastName", mrzData.getLastName());
            response.put("passportNumber", mrzData.getPassportNumber());
            response.put("nationality", mrzData.getNationality());
            response.put("dateOfBirth", mrzData.getDateOfBirth());
            response.put("gender", mrzData.getGender());
            response.put("expiryDate", mrzData.getExpiryDate());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "OCR processing failed"));
        }
    }

}
