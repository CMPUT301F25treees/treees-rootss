package com.example.myapplication.features.user;

public class UPastEventItem {

    private String eventId;
    private String title;
    private String priceDisplay;
    private String date;
    private String status;

    public UPastEventItem() {}

    public UPastEventItem(String eventId, String title,
                          String priceDisplay, String date, String status) {
        this.eventId = eventId;
        this.title = title;
        this.priceDisplay = priceDisplay;
        this.date = date;
        this.status = status;
    }

    public String getEventId()      { return eventId; }
    public String getTitle()        { return title; }
    public String getPriceDisplay() { return priceDisplay; }
    public String getDate()         { return date; }
    public String getStatus()       { return status; }
}
