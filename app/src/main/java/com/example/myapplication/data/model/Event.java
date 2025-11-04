package com.example.myapplication.data.model;

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

    public String getQrData() {
        return qrData;
    }

    public void setQrData(String qrData) {
        this.qrData = qrData;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String id) {
        this.eventId = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getStartDateMillis() {
        return startDateMillis;
    }

    public void setStartDateMillis(long startDateMillis) {
        this.startDateMillis = startDateMillis;
    }

    public long getEndDateMillis() {
        return endDateMillis;
    }

    public void setEndDateMillis(long endDateMillis) {
        this.endDateMillis = endDateMillis;
    }

    public long getSelectionDateMillis() {
        return selectionDateMillis;
    }

    public void setSelectionDateMillis(long selectionDateMillis) {
        this.selectionDateMillis = selectionDateMillis;
    }

    public int getEntrantsToDraw() {
        return entrantsToDraw;
    }

    public void setEntrantsToDraw(int entrantsToDraw) {
        this.entrantsToDraw = entrantsToDraw;
    }

    public boolean isGeoRequired() {
        return geoRequired;
    }

    public void setGeoRequired(boolean geoRequired) {
        this.geoRequired = geoRequired;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public Event(){

    }
    public Event(String title, String address, String descr, int capacity,
                 long startDateMillis, long endDateMillis, long selectionDateMillis,
                 int entrantsToDraw, boolean geoRequired, String posterUrl, String organizerId, String qrData) {
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
    }
}
