package com.example.shareplate;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Event implements Serializable {

    private String name, desc, date, time, typeOfEvents, seatAvailable, location, imageUrl, documentId, ownerProfileImageUrl, email, ownerUsername;

    private final int imageResourceId;
    private long createdAt;
    
    // Required empty constructor for Firebase
    public Event() {
        this.imageResourceId = 0; // Default value for final field
    }
    
    public Event(String name, String desc, String date, String time, String typeOfEvents, String seatAvailable, String location, int img, String imageUrl, String ownerProfileImageUrl, String email, String ownerUsername){

        this.name = name;
        this.desc = desc;
        this.date = date;
        this.time = time;
        this.typeOfEvents = typeOfEvents;
        this.seatAvailable = seatAvailable;
        this.location = location;
        this.imageResourceId = img;
        this.imageUrl = imageUrl;
        this.ownerProfileImageUrl = ownerProfileImageUrl;
        this.email = email;
        this.createdAt = System.currentTimeMillis();
        this.ownerUsername = ownerUsername;

    }
    public String getName(){
        return name;
    }

    public String getDescription(){
        return desc;
    }

    public String getDate(){
        return date;
    }

    public String getTime(){
        return time;
    }

    public String getTypeOfEvents(){
        return typeOfEvents;
    }

    public String getImageUrl() {return imageUrl;}

    public String getSeatAvailable(){
        return seatAvailable;
    }

    public String getLocation(){
        return location;
    }

    public int getImageResourceId(){
        return imageResourceId;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDocumentId() {return documentId; }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getOwnerProfileImageUrl() {
        return ownerProfileImageUrl;
    }

    public void setOwnerProfileImageUrl(String ownerProfileImageUrl) {
        this.ownerProfileImageUrl = ownerProfileImageUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedCreationDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.US);
        return sdf.format(new Date(createdAt));
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }
}
