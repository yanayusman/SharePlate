package com.example.shareplate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NonFoodItemRepository {
    private static final String DONATION_COLLECTION = "allDonationItems";
    private static final String REQUEST_COLLECTION = "requestFood";
    private static final String USERS_COLLECTION = "users";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public NonFoodItemRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public interface OnNonFoodItemsLoadedListener {
        void onNonFoodItemsLoaded(List<NonFoodItem> items);
        void onError(Exception e);
    }

    public void addNonFoodItem(NonFoodItem item) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            System.err.println("User is not logged in. Cannot add non-food item.");
            return;
        }

                    // Prepare the data to be added
                    Map<String, Object> nonFoodData = new HashMap<>();
                    nonFoodData.put("name", item.getName());
                    nonFoodData.put("category", item.getCategory());
                    nonFoodData.put("description", item.getDescription());
                    nonFoodData.put("quantity", item.getQuantity());
                    nonFoodData.put("pickupTime", item.getPickupTime());
                    nonFoodData.put("location", item.getLocation());
                    nonFoodData.put("imageResourceID", item.getImageResourceId());
                    nonFoodData.put("imageUrl", item.getImageUrl());
                    nonFoodData.put("email", item.getEmail());
                    nonFoodData.put("status", item.getStatus());
                    nonFoodData.put("createdAt", System.currentTimeMillis());
                    nonFoodData.put("donateType", "NonFood");

                    // Save the data to the database
                    db.collection(DONATION_COLLECTION)
                            .add(nonFoodData)
                            .addOnSuccessListener(documentReference -> {
                                String docId = documentReference.getId();
                                item.setDocumentId(docId); // Set the document ID in the item
                                System.out.println("Document added with ID: " + docId);
                            })
                            .addOnFailureListener(e -> {
                                System.err.println("Error adding document: " + e);
                            });
    }

    public void requestFoodItem(Map<String, Object> requestData) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            System.err.println("User not logged in");
            return;
        }

        String userEmail = currentUser.getEmail();

        db.collection(USERS_COLLECTION).document(userEmail).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        if (username != null) {
                            requestData.put("requestedBy", username);
                            db.collection(REQUEST_COLLECTION).add(requestData)
                                    .addOnSuccessListener(documentReference -> {
                                        System.out.println("Request added with ID: " + documentReference.getId());
                                    })
                                    .addOnFailureListener(e -> {
                                        System.err.println("Error adding request: " + e);
                                    });
                        } else {
                            System.err.println("Username not found for current user");
                        }
                    } else {
                        System.err.println("User document does not exist");
                    }
                })
                .addOnFailureListener(e -> {
                    System.err.println("Error retrieving user document: " + e);
                });
    }

    public void deleteNonFoodItem(String documentId, OnDeleteCompleteListener listener) {
        if (documentId == null) {
            System.err.println("Cannot delete item: document ID is null");
            if (listener != null) {
                listener.onDeleteFailure(new Exception("Document ID is null"));
            }
            return;
        }

        db.collection(DONATION_COLLECTION)
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

        db.collection(DONATION_COLLECTION)
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
}
