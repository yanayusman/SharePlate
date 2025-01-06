package com.example.shareplate;

public class User {
    private String username, email, phoneNumber, favourite, eventName, eventType, eventDate, eventTime, eventLocation, ownerImg;

    public User(String username, String email, String phoneNumber, String eventName, String eventType, String eventDate, String eventTime, String eventLocation, String ownerProfileImg) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.eventName = eventName;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.eventLocation = eventLocation;
        this.ownerImg = ownerProfileImg;
    }

    public String getUsername(){ return username; }
    public String getEmail(){ return email; }
    public String getPhoneNumber(){ return phoneNumber; }
    public String getFavourite(){ return favourite; }
    public String getEventName(){ return eventName; }
    public String getEventType(){ return eventType; }
    public String getEventDate(){ return eventDate; }
    public String getEventTime(){ return eventTime; }
    public String getEventLocation(){ return eventLocation; }
    public String getOwnerImg(){ return ownerImg; }

    public void setUsername(String username){
        this.username = username;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber){
        this.email = email;
    }

    public void setFavourite(String fav){
        this.favourite = fav;
    }

    public void setEventName(String name){
        this.eventName = name;
    }

    public void setEventType(String type){
        this.eventType = type;
    }

    public void setEventDate(String date){
        this.eventDate = date;
    }

    public void setEventTime(String time){
        this.eventTime = time;
    }

    public void setEventLocation(String location){
        this.eventLocation = location;
    }

    public void setOwnerImg(String ownerImg){
        this.ownerImg = ownerImg;
    }
}