package com.example.myapplication.features.user;

/**
 * Simple view-model representing a single past event item displayed in the UI.
 * <p>
 * This class is typically used by list or adapter components to render
 * information about an event the user has previously attended or interacted with.
 */
public class UPastEventItem {

    /** Unique identifier of the event (e.g., Firestore document ID). */
    private String eventId;

    /** Display title of the event. */
    private String title;

    /** Human-readable price information for the event (e.g., "Free", "$10.00"). */
    private String priceDisplay;

    /** Display string for the event date (already formatted for the UI). */
    private String date;

    /** Status of the past event (e.g., "Attended", "Cancelled"). */
    private String status;

    /**
     * No-argument constructor required for certain serialization frameworks
     * (e.g., Firestore, Gson) when deserializing objects.
     */
    public UPastEventItem() {}

    /**
     * Creates a new {@code UPastEventItem} with all displayable properties.
     *
     * @param eventId      unique identifier of the event
     * @param title        display title of the event
     * @param priceDisplay formatted price string for the event
     * @param date         formatted date string for when the event occurred
     * @param status       status describing the outcome or state of the event
     */
    public UPastEventItem(String eventId, String title,
                          String priceDisplay, String date, String status) {
        this.eventId = eventId;
        this.title = title;
        this.priceDisplay = priceDisplay;
        this.date = date;
        this.status = status;
    }

    /**
     * Returns the unique identifier for this event.
     *
     * @return the event ID
     */
    public String getEventId()      { return eventId; }

    /**
     * Returns the display title of this event.
     *
     * @return the title of the event
     */
    public String getTitle()        { return title; }

    /**
     * Returns the formatted price string for this event.
     *
     * @return the price display text
     */
    public String getPriceDisplay() { return priceDisplay; }

    /**
     * Returns the formatted date string for when the event occurred.
     *
     * @return the event date text
     */
    public String getDate()         { return date; }

    /**
     * Returns the status of this past event (e.g., attended, cancelled).
     *
     * @return the status string
     */
    public String getStatus()       { return status; }
}
