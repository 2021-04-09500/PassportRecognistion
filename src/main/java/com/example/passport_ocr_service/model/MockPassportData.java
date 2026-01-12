package com.example.passport_ocr_service.model;

public class MockPassportData {

    public static PassportData sample() {
        PassportData data = new PassportData();

        data.setFirstName("JOHN");
        data.setLastName("DOE");
        data.setPassportNumber("A12345678");
        data.setNationality("USA");
        data.setDateOfBirth("1990-01-01");
        data.setGender("Male");
        data.setExpiryDate("2030-01-01");

        return data;
    }
}

