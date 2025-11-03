package com.example.myapplication.features.user;

import androidx.annotation.ColorInt;

public class UserEvent {
    private final String id;
    private final String name;
    private final String location;
    private final String instructor;
    private final String price;
    private final String descr;
    private final long endTimeMillis;
    private final int bannerColor;

    public UserEvent(String id, String name, String location, String instructor,
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

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getInstructor() { return instructor; }
    public String getPrice() { return price; }
    public String getDescr(){ return descr; }
    public long getEndTimeMillis() { return endTimeMillis; }
    public int getBannerColor() { return bannerColor; }
}
