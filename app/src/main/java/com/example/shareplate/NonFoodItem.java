package com.example.shareplate;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NonFoodItem implements Serializable {

    private String name, category, description, quantity, pickupTime, location, imageUrl, email, documentId, status, ownerProfileImageUrl, donateType;
    private int imageResourceId;
    private long createdAt;

    public NonFoodItem(String name, String category, String description, String quantity, String pickupTime, String location, int imageResourceId, String imageUrl, String email, String ownerProfileImageUrl, String donateType){

        this.name = name;
        this.category = category;
        this.description = description;
        this.quantity = quantity;
        this.pickupTime = pickupTime;
        this.location = location;
        this.imageResourceId = imageResourceId;
        this.imageUrl = imageUrl;
        this.email = email;
        this.documentId = null; // Will be set after Firestore creates the document
        this.status = "active"; // Default status
        this.createdAt = System.currentTimeMillis(); // Set current time as default
        this.ownerProfileImageUrl = ownerProfileImageUrl;
        this.donateType = donateType;

    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
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

    public String getEmail() {
        return email;
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
}