package com.example.myapplication.features.user;

import androidx.annotation.ColorInt;

class UserEvent {
    private final String id;
    private final String name;
    private final String location;
    private final String instructor;
    private final String price;
    private final String descr;
    private final long endTimeMillis;
    private final int bannerColor;

    UserEvent(String id, String name, String location, String instructor,
              String price, String descr, long endTimeMillis, @ColorInt int bannerColor) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.instructor = instructor;
        this.price = price;
        this.descr = descr;
        this.endTimeMillis = endTimeMillis;
        this.bannerColor = bannerColor;
    }

    String getId() { return id; }
    String getName() { return name; }
    String getLocation() { return location; }
    String getInstructor() { return instructor; }
    String getPrice() { return price; }
    String getDescr(){ return descr; }
    long getEndTimeMillis() { return endTimeMillis; }
    int getBannerColor() { return bannerColor; }
}
