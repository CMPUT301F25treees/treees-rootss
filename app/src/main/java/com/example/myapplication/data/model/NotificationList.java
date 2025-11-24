package com.example.myapplication.data.model;

import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a model for the NotificationList in Firestore
 *
 * Fields of notificationList document:
 * - eventId: String
 * - all: List<String> This is a list of all users for notification purposes *Could possibly be changed*
 * - cancelled: List<String> This is a list of all users who chose not to accept the invitation from an event
 * - invited: List<String> This is a list of all users who are chosen by the lottery to sign up for an event
 * - finalList: List<String> This is a list of all users who have accepted the invitation to the event.
 * - waiting: List<String> This is a list of all users who are a part of the waiting list. This list gets
 *   updated as users are moved to the cancelled or final list.
 */
public class NotificationList {

    private String eventId;
    private List<String> all;
    private List<String> cancelled;
    private List<String> invited;
    private List<String> waiting;
    private List<String> finalList;

    public NotificationList() {
        this.all = new ArrayList<>();
        this.cancelled = new ArrayList<>();
        this.invited = new ArrayList<>();
        this.waiting = new ArrayList<>();
        this.finalList = new ArrayList<>();
    }

    public NotificationList(String eventId) {
        this();
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public List<String> getAll() {
        return all;
    }

    public void setAll(List<String> all) {
        this.all = all;
    }

    public List<String> getCancelled() {
        return cancelled;
    }

    public void setCancelled(List<String> cancelled) {
        this.cancelled = cancelled;
    }

    public List<String> getInvited() {
        return invited;
    }

    public void setInvited(List<String> invited) {
        this.invited = invited;
    }

    public List<String> getWaiting() {
        return waiting;
    }

    public void setWaiting(List<String> waiting) {
        this.waiting = waiting;
    }

    @PropertyName("final")
    public List<String> getFinalList() {
        return finalList;
    }

    @PropertyName("final")
    public void setFinalList(List<String> finalList) {
        this.finalList = finalList;
    }

}
