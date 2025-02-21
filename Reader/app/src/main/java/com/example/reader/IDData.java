package com.example.reader;

import com.google.gson.Gson;


public class IDData {
    private String name;
    private String dob;
    private String idNumber;

    public IDData(String name, String dob, String idNumber) {
        this.name = name;
        this.dob = dob;
        this.idNumber = idNumber;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static IDData fromJson(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, IDData.class);
    }

    public String getName() {
        return name;
    }

    public String getDob() {
        return dob;
    }

    public String getIdNumber() {
        return idNumber;
    }
}
