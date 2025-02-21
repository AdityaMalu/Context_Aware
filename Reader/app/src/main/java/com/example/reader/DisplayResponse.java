package com.example.reader;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "MessageResponse")
public class DisplayResponse {

    @Element(name = "message")
    private String message;


    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
