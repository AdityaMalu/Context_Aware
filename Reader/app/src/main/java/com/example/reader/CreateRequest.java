package com.example.reader;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "CreateRequest")
public class CreateRequest {

    @Element(name = "index")
    String index;
    @Element(name = "data")
    String encryptedData;

    @Element(name = "accessId")
    String appID;


    public CreateRequest(String index, String encryptedData, String appID){
        this.index = index;
        this.encryptedData = encryptedData;
        this.appID = appID;
    }
}
