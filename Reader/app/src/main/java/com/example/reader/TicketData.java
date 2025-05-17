package com.example.reader;

import com.google.gson.Gson;

public class TicketData {
    private String eventType;
    private String ticketId;
    private String seatNo;
    private String dateOfEvent;

    public TicketData(String eventType, String ticketId, String seatNo, String dateOfEvent) {
        this.eventType = eventType;
        this.ticketId = ticketId;
        this.seatNo = seatNo;
        this.dateOfEvent = dateOfEvent;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static TicketData fromJson(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, TicketData.class);
    }

    public String getEventType() {
        return eventType;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getSeatNo() {
        return seatNo;
    }

    public String getDateOfEvent() {
        return dateOfEvent;
    }
}
