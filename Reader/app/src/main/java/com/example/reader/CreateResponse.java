package com.example.reader;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "MessageResponse")
public class CreateResponse {

    @Element(name = "message")
    String message;

    void setMessage(String message){
        this.message = message;
    }

    String getMessage(){
        return this.message;
    }


}
