package com.example.shareplate;

import java.io.Serializable;

// This class represents a single notification item.
// It stores the data for each notification, such as the title, message, timestamp, and an icon resource ID.
public class Notification implements Serializable {
    private String title, message, timestamp, location, imgUrl;
    // Constructor
    public Notification(String title, String message, String timestamp, String location, String imgUrl) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.location = location;
        this.imgUrl = imgUrl;
    }
    // Getters
    public String getTitle() {
        return title;
    }
    public String getMessage() {
        return message;
    }
    public String getTimestamp() {
        return timestamp;
    }
    public String getLocation(){
        return location;
    }
    public String getImgUrl() {
        return imgUrl;
    }
}