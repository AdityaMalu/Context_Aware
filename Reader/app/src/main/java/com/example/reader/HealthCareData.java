package com.example.reader;
import com.google.gson.Gson;
public class HealthCareData {
    private String bloodType;
    private String allergy;
    private String vaccination;
    private String medicalHistory;

    public HealthCareData(String bloodType, String allergy, String vaccination, String medicalHistory) {
        this.bloodType = bloodType;
        this.allergy = allergy;
        this.vaccination = vaccination;
        this.medicalHistory = medicalHistory;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static HealthCareData fromJson(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, HealthCareData.class);
    }

    public String getBloodType() {
        return bloodType;
    }

    public String getAllergy() {
        return allergy;
    }

    public String getVaccination() {
        return vaccination;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

}
