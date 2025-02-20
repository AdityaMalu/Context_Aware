package com.example.reader;

public interface APDUCommandListner {
    void sendApduCommand(byte[] command);
}
