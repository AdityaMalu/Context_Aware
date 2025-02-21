package com.example.reader;


import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "DisplayRequest")
public class DisplayRequest {

    @Element(name = "index")
    String index;

    @Element(name = "accessId")
    String accessID;

    DisplayRequest(String index, String accessID){
        this.index = index;
        this.accessID = accessID;
    }
}
