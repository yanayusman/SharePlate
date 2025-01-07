package com.example.shareplate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import android.util.Log;

public class DonationItemRepository {
    private static final String TAG = "DonationItemRepository";
    private static final String COLLECTION_NAME = "allDonationItems";
    private final FirebaseFirestore db;

    // Define the interface for callbacks
    public interface OnDonationCompleteListener {
        void onDonationSuccess();
        void onDonationFailure(Exception e);
    }

    public DonationItemRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface OnDonationItemsLoadedListener {
        void onDonationItemsLoaded(List<DonationItem> items);
        void onError(Exception e);
    }

    public void getAllDonationItems(OnDonationItemsLoadedListener listener) {
        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DonationItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String name = document.getString("name");
                            String foodCategory = document.getString("foodCategory");
                            String description = document.getString("description");
                            String category = document.getString("category");
                            String expiredDate = document.getString("expiredDate");
                            String quantity = document.getString("quantity");
                            String pickupTime = document.getString("pickupTime");
                            String location = document.getString("location");
                            String ownerEmail = document.getString("email");
                            String status = document.getString("status");
                            String ownerProfileImageUrl = document.getString("ownerProfileImageUrl");
                            String donateType = document.getString("donateType");
                            String feedback = document.getString("feedback");
                            String receiverEmail = document.getString("receiverEmail");

                            int imageResourceId = R.drawable.placeholder_image;
                            Long resourceIdLong = document.getLong("imageResourceID");
                            if (resourceIdLong != null) {
                                imageResourceId = resourceIdLong.intValue();
                            }

                            String imageUrl = document.getString("imageUrl");

                            Long createdAt = document.getLong("createdAt");

                            if (name != null && !name.isEmpty()) {
                                DonationItem item = new DonationItem(
                                        name,
                                        foodCategory != null ? foodCategory : "",
                                        description != null ? description : "",
                                        category != null ? category : "",
                                        expiredDate != null ? expiredDate : "",
                                        quantity != null ? quantity : "",
                                        pickupTime != null ? pickupTime : "",
                                        location != null ? location : "",
                                        imageResourceId,
                                        imageUrl,
                                        donateType,
                                        ownerProfileImageUrl != null ? ownerProfileImageUrl : "",
                                        ownerEmail
                                );
                                // Set the document ID and status
                                item.setDocumentId(document.getId());
                                item.setStatus(status != null ? status : "active");
                                item.setFeedback(feedback);
                                item.setReceiverEmail(receiverEmail);
                                if (createdAt != null) {
                                    item.setCreatedAt(createdAt);
                                }
                                items.add(item);
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing document: " + e.getMessage());
                        }
                    }
                    listener.onDonationItemsLoaded(items);
                })
                .addOnFailureListener(listener::onError);
    }

    public void addDonationItem(DonationItem item, OnDonationCompleteListener listener) {
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found");
            listener.onDonationFailure(new Exception("User not authenticated"));
            return;
        }

        String userEmail = currentUser.getEmail();

        if (userEmail == null || userEmail.isEmpty()) {
            System.err.println("User email is null or empty. Cannot fetch username.");
            return;
        }

        String ownerProfileImageUrl = currentUser.getPhotoUrl() != null ?
                currentUser.getPhotoUrl().toString() : "";

        // Create donation data map
        Map<String, Object> donationData = new HashMap<>();
        donationData.put("name", item.getName());
        donationData.put("foodCategory", item.getFoodCategory());
        donationData.put("expiredDate", item.getExpiredDate());
        donationData.put("quantity", item.getQuantity());
        donationData.put("pickupTime", item.getPickupTime());
        donationData.put("location", item.getLocation());
        donationData.put("imageResourceId", item.getImageResourceId());
        donationData.put("imageUrl", item.getImageUrl());
        donationData.put("email", userEmail);
        donationData.put("ownerProfileImageUrl", ownerProfileImageUrl);
        donationData.put("status", "active");
        donationData.put("createdAt", System.currentTimeMillis());
        donationData.put("donateType", "Food");

        // Add to Firestore
        db.collection(COLLECTION_NAME)
                .add(donationData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                    listener.onDonationSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    listener.onDonationFailure(e);
                });
    }


    public void deleteDonationItem(String documentId, OnDeleteCompleteListener listener) {
        if (documentId == null) {
            System.err.println("Cannot delete item: document ID is null");
            if (listener != null) {
                listener.onDeleteFailure(new Exception("Document ID is null"));
            }
            return;
        }

        System.out.println("Attempting to delete document with ID: " + documentId);

        db.collection(COLLECTION_NAME)
                .document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    System.out.println("Successfully deleted document: " + documentId);
                    if (listener != null) {
                        listener.onDeleteSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    System.err.println("Failed to delete document: " + documentId + ", error: " + e.getMessage());
                    if (listener != null) {
                        listener.onDeleteFailure(e);
                    }
                });
    }

    public interface OnDeleteCompleteListener {
        void onDeleteSuccess();
        void onDeleteFailure(Exception e);
    }

    public void updateDonationStatus(String documentId, String status, OnStatusUpdateListener listener) {
        if (documentId == null) {
            if (listener != null) {
                listener.onUpdateFailure(new Exception("Document ID is null"));
            }
            return;
        }

        db.collection(COLLECTION_NAME)
                .document(documentId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) {
                        listener.onUpdateSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onUpdateFailure(e);
                    }
                });
    }

    public interface OnStatusUpdateListener {
        void onUpdateSuccess();
        void onUpdateFailure(Exception e);
    }

    public void updateDonationWithFields(String documentId, Map<String, Object> updates, 
            OnStatusUpdateListener listener) {
        db.collection(COLLECTION_NAME)
                .document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    listener.onUpdateSuccess();
                })
                .addOnFailureListener(listener::onUpdateFailure);
    }
} 