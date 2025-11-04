package com.example.myapplication.data.model;

public class Event {
    public String id;
    public String title;
    public String address;
    public String desc;
    public int capacity;
    public double price;

    public long startDateMillis;
    public long endDateMillis;
    public long selectionDateMillis;

    public int entrantsToDraw;
    public boolean geoRequired;
    public String posterUrl;
    public String organizerId;

    public Event(){

    }
}
