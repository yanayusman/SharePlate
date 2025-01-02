package com.example.shareplate;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RequestNonFood implements Serializable {
    private String name,itemCategory, urgencyLevel, quantity, pickupTime, location, imageUrl, ownerUsername, documentId, status, ownerProfileImageUrl, donateType, email;
    private int imageResourceId;
    private long createdAt;

    // Constructor
    public RequestNonFood() {
        // Required empty constructor for Firestore
    }

    public RequestNonFood(String name, String itemCategory, String urgencyLevel, String quantity, String pickupTime, String location, int imageResourceId, String imageUrl, String ownerUsername, String donateType, String ownerProfileImageUrl, String email) {
        this.name = name;
        this.itemCategory = itemCategory;
        this.urgencyLevel = urgencyLevel;
        this.quantity = quantity;
        this.pickupTime = pickupTime;
        this.location = location;
        this.imageResourceId = imageResourceId;
        this.imageUrl = imageUrl;
        this.ownerUsername = ownerUsername;
        this.ownerProfileImageUrl = ownerProfileImageUrl;
        this.status = "active";
        this.createdAt = System.currentTimeMillis();
        this.donateType = donateType;
        this.email = email;
    }

    // Getter methods
    public String getName() {
        return name;
    }

    public String getItemCategory() {
        return itemCategory;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }


    public String getQuantity() {
        return quantity;
    }

    public String getPickupTime() {
        return pickupTime;
    }

    public String getLocation() {
        return location;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getOwnerProfileImageUrl() {
        return ownerProfileImageUrl;
    }

    public void setOwnerProfileImageUrl(String ownerProfileImageUrl) {
        this.ownerProfileImageUrl = ownerProfileImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDonateType(){
        return donateType;
    }

    public void setDonateType(String donateType){
        this.donateType = donateType;
    }

    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email = email;
    }
}