package com.example.shareplate;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// This class represents a single notification item.
// It stores the data for each notification, such as the title, message, timestamp, and an icon resource ID.
public class Notification implements Serializable {
    private String title, message, location, imgUrl, ownerEmail, requesterEmail, activityType, expiredDate, notiType;
    private long timestamp;
    // Constructor
    public Notification(String title, String message, long timestamp, String location, String imgUrl, String ownerEmail, String requesterEmail, String activityType, String expiredDate, String notiType) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.location = location;
        this.imgUrl = imgUrl;
        this.ownerEmail = ownerEmail;
        this.requesterEmail = requesterEmail;
        this.activityType = activityType;
        this.expiredDate = expiredDate;
        this.notiType = notiType;
    }
    // Getters
    public String getTitle() {
        return title;
    }
    public String getMessage() {
        return message;
    }
    public String getTimestamp() {
        long millis = Long.parseLong(String.valueOf(timestamp));
        Instant instant = Instant.ofEpochMilli(millis);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        return formatter.format(instant);
    }
    public String getLocation(){
        return location;
    }
    public String getImgUrl() {
        return imgUrl;
    }
    public String getOwnerEmail(){
        return ownerEmail;
    }
    public String getRequesterEmail(){
        return requesterEmail;
    }
    public String getActivityType(){
        return activityType;
    }
    public String getExpiredDate() { return expiredDate; }
    public String getNotiType() { return notiType; }
}