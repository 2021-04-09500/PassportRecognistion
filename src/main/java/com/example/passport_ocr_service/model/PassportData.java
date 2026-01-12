package com.example.passport_ocr_service.model;

import lombok.Data;

@Data
public class PassportData {
    private String firstName;
    private String lastName;
    private String passportNumber;
    private String nationality;
    private String dateOfBirth;
    private String gender;
    private String expiryDate; // optional, can extract from MRZ if needed
}
