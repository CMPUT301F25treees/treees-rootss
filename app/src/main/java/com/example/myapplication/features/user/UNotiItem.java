package com.example.myapplication.features.user;

import com.google.firebase.Timestamp;

import java.util.List;

/**
 * This class represents a single user notification that is retrieved from the Firestore
 */
public class UNotiItem {
    private String from;
    private String message;
    private String event;
    private String eventId;
    private String type;
    private String status;  // "pending", "accepted", "declined"
    private Timestamp dateMade;
    private List<String> uID;
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

    public String getEventId() { return eventId; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public Timestamp getDateMade() { return dateMade; }
    public List<String> getUID() { return uID; }

    public boolean isPending() { return "pending".equals(status); }
    public boolean isAccepted() { return "accepted".equals(status); }
    public boolean isDeclined() { return "declined".equals(status); }
    public boolean isLotteryWin() { return "lottery_win".equals(type); }

}
