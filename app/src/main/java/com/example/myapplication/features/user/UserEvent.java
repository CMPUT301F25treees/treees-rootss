package com.example.myapplication.features.user;

import androidx.annotation.ColorInt;

import java.util.List;

public class UserEvent {
    private String id;
    private String organizerID;
    private  String name;
    private  String location;
    private  String instructor;
    private  int price;
    private  String descr;
    private  long endTimeMillis;
    private  int bannerColor;
    private  List<String> waitlist;
    private boolean geoRequired;
    private int capacity;
    private long startTimeMillis;
    private long selectionDateMillis;
    private int entrantsToDraw;
    private String posterUrl;
    private String qrData;
    private String imageUrl;

    /**
     * This method is required for Firestore to construct the object
     */
    public UserEvent() {}

    UserEvent(String id, String organizerID, String name, String location, String instructor,
              int price, String descr, long endTimeMillis, @ColorInt int bannerColor, List<String> waitlist) {
        this.id = id;
        this.organizerID = organizerID;
        this.name = name;
        this.location = location;
        this.instructor = instructor;
        this.price = price;
        this.descr = descr;
        this.endTimeMillis = endTimeMillis;
        this.bannerColor = bannerColor;
        this.waitlist = waitlist;
    }

    public String getId() { return id; }
    public String getOrganizerID() { return organizerID; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getInstructor() { return instructor; }
    public int getPrice() { return price; }
    public String getDescr(){ return descr; }
    public List<String> getWaitlist() { return waitlist; }
    public long getEndTimeMillis() { return endTimeMillis; }
    public int getBannerColor() { return bannerColor; }

    public void setId(String id) { this.id = id; }
    public void setOrganizerID(String organizerID) {this.organizerID = organizerID;}
    public void setName(String name) { this.name = name; }
    public void setLocation(String location) { this.location = location; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
    public void setPrice(int price) { this.price = price; }
    public void setDescr(String descr) { this.descr = descr; }
    public void setEndTimeMillis(long endTimeMillis) { this.endTimeMillis = endTimeMillis; }
    public void setBannerColor(int bannerColor) { this.bannerColor = bannerColor; }
    public void setWaitlist(List<String> waitlist) { this.waitlist = waitlist; }

    public void setGeoRequired(boolean geoRequired){ this.geoRequired = geoRequired; }
    public boolean isGeoRequired() {
        return geoRequired;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
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

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getQrData() {
        return qrData;
    }

    public void setQrData(String qrData) {
        this.qrData = qrData;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}