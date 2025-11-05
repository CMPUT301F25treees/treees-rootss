package com.example.myapplication.features.user;

public class UNotiItem {
    private String from;
    private String message;
    private String event;

    public UNotiItem() {}
    public UNotiItem(String from, String message, String event) {
        this.from = from;
        this.message = message;
        this.event = event;
    }

    //Getters
    public String getFrom() {
        return from;
    }

    public String getMessage() {
        return message;
    }

    public String getEvent() {
        return event;
    }

}
