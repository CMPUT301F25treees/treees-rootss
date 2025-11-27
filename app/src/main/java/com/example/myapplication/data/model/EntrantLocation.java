package com.example.myapplication.data.model;

public class EntrantLocation {
    private String uid;
    private double lat;
    private double lng;

    public EntrantLocation() {}

    /**
     * @param uid
     * @param lat
     * @param lng
     */
    public EntrantLocation(String uid, double lat, double lng) {
        this.uid = uid;
        this.lat = lat;
        this.lng = lng;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
}
