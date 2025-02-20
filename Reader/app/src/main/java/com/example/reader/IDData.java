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
}
