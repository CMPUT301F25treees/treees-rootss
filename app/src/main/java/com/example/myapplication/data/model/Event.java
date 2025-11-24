package com.example.myapplication.data.model;

import java.util.List;

public class Event {
    private String eventId;
    private String title;
    private String address;
    private String descr;
    private int capacity;
    private double price;

    private long startDateMillis;
    private long endDateMillis;
    private long selectionDateMillis;

    private int entrantsToDraw;
    private boolean geoRequired;
    private String posterUrl;
    private String organizerId;
    private String qrData;
    private String theme;

    private List<String> waitlist;

    /**
     * Get the QR data associated with the event.
     * @param None
     * @return the QR data as a String
     */
    public String getQrData() {
        return qrData;
    }

    /**
     * Set the QR data for the event.
     * @param qrData the QR data to set
     * @return void
     */
    public void setQrData(String qrData) {
        this.qrData = qrData;
    }

    /**
     * Get event id.
     * @param None
     * @return void
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Set event id.
     * @param id the event id to set
     * @return void
     */
    public void setEventId(String id) {
        this.eventId = id;
    }

    /**
     * Empty constructor for Firebase
     * @param None
     * @return title of the event
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set title of the event.
     * @param title
     * @return void
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get address of the event.
     * @param None
     * @return address of the event
     */
    public String getAddress() {
        return address;
    }

    /**
     * Set address of the event.
     * @param address
     * @return void
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Get description of the event.
     * @param None
     * @return description of the event
     */
    public String getDescr() {
        return descr;
    }

    /**
     * Set description of the event.
     * @param descr
     * @return void
     */
    public void setDescr(String descr) {
        this.descr = descr;
    }

    /**
     * Get capacity of the event.
     * @param None
     * @return capacity of the event
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Set capacity of the event.
     * @param capacity
     * @return void
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Get price of the event.
     * @param None
     * @return price of the event
     */
    public double getPrice() {
        return price;
    }

    /**
     * Set price of the event.
     * @param price
     * @return void
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Get start date in milliseconds.
     * @param None
     * @return start date in milliseconds
     */
    public long getStartDateMillis() {
        return startDateMillis;
    }

    /**
     * Set start date in milliseconds.
     * @param startDateMillis
     * @return void
     */
    public void setStartDateMillis(long startDateMillis) {
        this.startDateMillis = startDateMillis;
    }

    /**
     * Get end date in milliseconds.
     * @param None
     * @return end date in milliseconds
     */
    public long getEndDateMillis() {
        return endDateMillis;
    }

    /**
     * Set end date in milliseconds.
     * @param endDateMillis
     * @return void
     */
    public void setEndDateMillis(long endDateMillis) {
        this.endDateMillis = endDateMillis;
    }

    /**
     * Get selection date in milliseconds.
     * @param None
     * @return selection date in milliseconds
     */
    public long getSelectionDateMillis() {
        return selectionDateMillis;
    }

    /**
     * Set selection date in milliseconds.
     * @param selectionDateMillis
     * @return void
     */
    public void setSelectionDateMillis(long selectionDateMillis) {
        this.selectionDateMillis = selectionDateMillis;
    }

    /**
     * Get number of entrants to draw.
     * @param None
     * @return number of entrants to draw
     */
    public int getEntrantsToDraw() {
        return entrantsToDraw;
    }

    /**
     * Set number of entrants to draw.
     * @param entrantsToDraw
     * @return void
     */
    public void setEntrantsToDraw(int entrantsToDraw) {
        this.entrantsToDraw = entrantsToDraw;
    }

    /**
     * Check if geo is required.
     * @param None
     * @return true if geo is required, false otherwise
     */
    public boolean isGeoRequired() {
        return geoRequired;
    }

    /**
     * Set whether geo is required.
     * @param geoRequired
     * @return void
     */
    public void setGeoRequired(boolean geoRequired) {
        this.geoRequired = geoRequired;
    }

    /**
     * Get poster URL.
     * @param None
     * @return poster URL
     */
    public String getPosterUrl() {
        return posterUrl;
    }

    /**
     * Set poster URL.
     * @param posterUrl
     * @return void
     */
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    /**
     * Get organizer ID.
     * @param None
     * @return organizer ID
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Set organizer ID.
     * @param organizerId
     * @return void
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * Get theme of the event.
     * @param None
     * @return theme of the event
     */
    public String getTheme() {
        return theme;
    }

    /**
     * Set theme of the event.
     * @param theme
     * @return void
     */
    public void setTheme(String theme) {
        this.theme = theme;
    }

    /**
     * Get waitlist of the event.
     * @param None
     * @return waitlist of the event
     */
    public List<String> getWaitlist() {
        return waitlist;
    }

    /**
     * Set waitlist of the event.
     * @param waitlist
     * @return void
     */
    public void setWaitlist(List<String> waitlist) {
        this.waitlist = waitlist;
    }

    /**
     * Constructor for Event class.
     * @param title
     * @param address
     * @param descr
     * @param capacity
     * @param startDateMillis
     * @param endDateMillis
     * @param selectionDateMillis
     * @param entrantsToDraw
     * @param geoRequired
     * @param posterUrl
     * @param organizerId
     * @param qrData
     * @param waitlist
     * @param theme
     * @return void
     */
    public Event(String title, String address, String descr, int capacity,
                 long startDateMillis, long endDateMillis, long selectionDateMillis,
                 int entrantsToDraw, boolean geoRequired, String posterUrl, String organizerId,
                 String qrData, List<String> waitlist, String theme) {
        this.title = title;
        this.address = address;
        this.descr = descr;
        this.capacity = capacity;
        this.startDateMillis = startDateMillis;
        this.endDateMillis = endDateMillis;
        this.selectionDateMillis = selectionDateMillis;
        this.entrantsToDraw = entrantsToDraw;
        this.geoRequired = geoRequired;
        this.posterUrl = posterUrl;
        this.organizerId = organizerId;
        this.qrData = qrData;
        this.waitlist = waitlist;
        this.theme = theme;
    }

}
