package com.example.shareplate;

public enum SortOption {
    DEFAULT("Default"),
    DATE_CREATED("Date Created"),
    EXPIRY_DATE("Expiry Date"),
    CATEGORY("Category"),
    QUANTITY("Quantity"),
    LOCATION("Location"),
    PICKUP_TIME("Pickup Time");

    private final String displayName;

    SortOption(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}