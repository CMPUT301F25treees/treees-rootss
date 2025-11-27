package com.example.myapplication.features.user;

/**
 *  Fragment that allows users to see past event items.
 */
public class UPastEventItem {

    /**
     *  Event item fields.
     */
    private String eventId;
    /**
     *  Title of the event.
     */
    private String title;
    /**
     *  Price of the event.
     */
    private String priceDisplay;
    /**
     *  Date of the event.
     */
    private String date;
    /**
     *  Status of the event.
     */
    private String status;

    public UPastEventItem() {}

    /**
     * @param eventId
     * @param title
     * @param priceDisplay
     * @param date
     * @param status
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
     * @return
     */
    public String getEventId()      { return eventId; }

    /**
     * @return
     */
    public String getTitle()        { return title; }

    /**
     * @return
     */
    public String getPriceDisplay() { return priceDisplay; }

    /**
     * @return
     */
    public String getDate()         { return date; }

    /**
     * @return
     */
    public String getStatus()       { return status; }
}
