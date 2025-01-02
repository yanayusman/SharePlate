package com.example.shareplate;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventRepo {
    private static final String TAG = "EventRepo";
    private static final String COLLECTION_NAME = "events";
    private final FirebaseFirestore db;

    public EventRepo() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface OnEventItemsLoadedListener {
        void onEventItemsLoaded(List<Event> events);
        void onError(Exception e);
    }

    public interface OnEventCompleteListener {
        void onEventSuccess();
        void onEventFailure(Exception e);
    }

    public void getAllEventItems(OnEventItemsLoadedListener listener, String email) {
                        // Now fetch events and attach the username
                        db.collection(COLLECTION_NAME)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    List<Event> items = new ArrayList<>();
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        String name = document.getString("name");
                                        String desc = document.getString("description");
                                        String date = document.getString("date");
                                        String time = document.getString("time");
                                        String typeOfEvents = document.getString("typeOfEvents");
                                        String seatAvailable = document.getString("seatsAvailable");
                                        String location = document.getString("location");
                                        Long imageResourceID = document.getLong("imageResourceID");
                                        int img = imageResourceID != null ? imageResourceID.intValue() : 0;
                                        String imageUrl = document.getString("imageUrl");
                                        String ownerImageUrl = document.getString("ownerImageUrl");
                                        String ownerEmail = document.getString("email");

                                        // Use the username fetched from the users collection
                                        Event event = new Event(name, desc, date, time, typeOfEvents, seatAvailable, location, img, imageUrl, ownerImageUrl, ownerEmail,"");

                                        event.setDocumentId(document.getId());
                                        items.add(event);
                                    }
                                    listener.onEventItemsLoaded(items);
                                })
                                .addOnFailureListener(listener::onError);
    }

    public void addEvent(String userEmail, Event item, EventRepo.OnEventCompleteListener listener) {
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found");
            listener.onEventFailure(new Exception("User not authenticated"));
            return;
        }

        String ownerProfileImageUrl = currentUser.getPhotoUrl() != null ?
                currentUser.getPhotoUrl().toString() : "";

                    // Create donation data map
                    Map<String, Object> donationData = new HashMap<>();
                    donationData.put("name", item.getName());
                    donationData.put("description", item.getDescription());
                    donationData.put("date", item.getDate());
                    donationData.put("time", item.getTime());
                    donationData.put("typeOfEvents", item.getTypeOfEvents());
                    donationData.put("seatsAvailable", item.getSeatAvailable());
                    donationData.put("location", item.getLocation());
                    donationData.put("imageResourceId", item.getImageResourceId());
                    donationData.put("imageUrl", item.getImageUrl());
                    donationData.put("email", userEmail);
                    donationData.put("ownerProfileImageUrl", ownerProfileImageUrl);
                    donationData.put("createdAt", System.currentTimeMillis());


                    // Add to Firestore
                    db.collection(COLLECTION_NAME)
                            .add(donationData)
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                                item.setDocumentId(documentReference.getId());
                                listener.onEventSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error adding document", e);
                                listener.onEventFailure(e);
                            });
    }


    public void deleteEvent(String documentId, EventRepo.OnDeleteCompleteListener listener) {
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
}