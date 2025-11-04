package com.example.myapplication.features.user;

import androidx.annotation.ColorInt;

import java.util.List;

public class UserEvent {
    private String id;
    private String organizerID;
    private  String name;
    private  String location;
    private  String instructor;
    private  String price;
    private  String descr;
    private  long endTimeMillis;
    private  int bannerColor;
    private  List<String> waitlist;


    /**
     * This method is required for Firestore to construct the object
     */
    public UserEvent() {}

    UserEvent(String id, String organizerID, String name, String location, String instructor,
              String price, String descr, long endTimeMillis, @ColorInt int bannerColor, List<String> waitlist) {
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
    public String getPrice() { return price; }
    public String getDescr(){ return descr; }
    public List<String> getWaitlist() { return waitlist; }
    public long getEndTimeMillis() { return endTimeMillis; }
    public int getBannerColor() { return bannerColor; }

    public void setId(String id) { this.id = id; }
    public void setOrganizerID(String organizerID) {this.organizerID = organizerID;}
    public void setName(String name) { this.name = name; }
    public void setLocation(String location) { this.location = location; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
    public void setPrice(String price) { this.price = price; }
    public void setDescr(String descr) { this.descr = descr; }
    public void setEndTimeMillis(long endTimeMillis) { this.endTimeMillis = endTimeMillis; }
    public void setBannerColor(int bannerColor) { this.bannerColor = bannerColor; }
    public void setWaitlist(List<String> waitlist) { this.waitlist = waitlist; }
}