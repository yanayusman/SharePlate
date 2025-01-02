package com.example.shareplate;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DonationItem implements Serializable {
    private String name,foodCategory, description, category, expiredDate, quantity, pickupTime, location, imageUrl, documentId, status, ownerProfileImageUrl, donateType, email;
    private int imageResourceId;
    private long createdAt;

    // Constructor
    public DonationItem() {
        // Required empty constructor for Firestore
    }

    public DonationItem(String name, String foodCategory, String description, String category, String expiredDate, String quantity, String pickupTime, String location, int imageResourceId, String imageUrl, String donateType, String ownerProfileImageUrl, String email) {
        this.name = name;
        this.foodCategory = foodCategory;
        this.description = description;
        this.category = category;
        this.expiredDate = expiredDate;
        this.quantity = quantity;
        this.pickupTime = pickupTime;
        this.location = location;
        this.imageResourceId = imageResourceId;
        this.imageUrl = imageUrl;
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

    public String getFoodCategory() {
        return foodCategory;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getExpiredDate() {
        return expiredDate;
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