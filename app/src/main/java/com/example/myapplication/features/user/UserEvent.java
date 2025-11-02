package com.example.myapplication.features.user;

import androidx.annotation.ColorInt;

import java.util.List;

class UserEvent {
    private final String id;
    private final String name;
    private final String location;
    private final String instructor;
    private final String price;
    private final String descr;
    private final long endTimeMillis;
    private final int bannerColor;
    private final List<String> waitlist;

    UserEvent(String id, String name, String location, String instructor,
              String price, String descr, long endTimeMillis, @ColorInt int bannerColor, List<String> waitlist) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.instructor = instructor;
        this.price = price;
        this.descr = descr;
        this.endTimeMillis = endTimeMillis;
        this.bannerColor = bannerColor;
        this.waitlist = waitlist;
    }

    String getId() { return id; }
    String getName() { return name; }
    String getLocation() { return location; }
    String getInstructor() { return instructor; }
    String getPrice() { return price; }
    String getDescr(){ return descr; }
    List<String> getWaitlist() { return waitlist; }
    long getEndTimeMillis() { return endTimeMillis; }
    int getBannerColor() { return bannerColor; }
}
