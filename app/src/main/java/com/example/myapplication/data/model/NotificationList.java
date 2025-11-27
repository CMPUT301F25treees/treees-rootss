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

    /** Default constructor required for calls to DataSnapshot.getValue(NotificationList.class)
     * @param None
     * @return void
     * */
    public NotificationList() {
        this.all = new ArrayList<>();
        this.cancelled = new ArrayList<>();
        this.invited = new ArrayList<>();
        this.waiting = new ArrayList<>();
        this.finalList = new ArrayList<>();
    }

    /** Constructor with eventId
     * @param eventId The ID of the event
     * @return void
     * */
    public NotificationList(String eventId) {
        this();
        this.eventId = eventId;
    }

    /** Get event id.
     * @param None
     * @return eventId The ID of the event
     * */
    public String getEventId() {
        return eventId;
    }

    /** Set event id.
     * @param eventId The ID of the event
     * @return void
     * */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /** Get all users.
     * @param None
     * @return List of all user IDs
     * */
    public List<String> getAll() {
        return all;
    }

    /** Set all users.
     * @param all List of all user IDs
     * @return void
     * */
    public void setAll(List<String> all) {
        this.all = all;
    }

    /** Get cancelled users.
     * @param None
     * @return List of cancelled user IDs
     * */
    public List<String> getCancelled() {
        return cancelled;
    }

    /** Set cancelled users.
     * @param cancelled List of cancelled user IDs
     * @return void
     * */
    public void setCancelled(List<String> cancelled) {
        this.cancelled = cancelled;
    }

    /** Get invited users.
     * @param None
     * @return List of invited user IDs
     * */
    public List<String> getInvited() {
        return invited;
    }

    /** Set invited users.
     * @param invited List of invited user IDs
     * @return void
     * */
    public void setInvited(List<String> invited) {
        this.invited = invited;
    }

    /** Get waiting users.
     * @param None
     * @return List of waiting user IDs
     * */
    public List<String> getWaiting() {
        return waiting;
    }

    /** Set waiting users.
     * @param waiting List of waiting user IDs
     * @return void
     * */
    public void setWaiting(List<String> waiting) {
        this.waiting = waiting;
    }

    /** Get final users.
     * @param None
     * @return List of final user IDs
     * */
    @PropertyName("final")
    public List<String> getFinalList() {
        return finalList;
    }

    /** Set final users.
     * @param finalList List of final user IDs
     * @return void
     * */
    @PropertyName("final")
    public void setFinalList(List<String> finalList) {
        this.finalList = finalList;
    }

}
