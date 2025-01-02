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

public class NotificationRepo {
    private static final String TAG = "NotificationRepo";
    private static final String COLLECTION_NAME = "notification";
    private final FirebaseFirestore db;

    public NotificationRepo() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface OnNotificationLoadedListener {
        void onNotificationLoaded(List<Notification> notification);
        void onError(Exception e);
    }

    public interface OnNotificationCompleteListener {
        void onNotificationSuccess();
        void onNotificationFailure(Exception e);
    }

    public void getAllNotification(NotificationRepo.OnNotificationLoadedListener listener) {
        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> items = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String title = document.getString("title");
                            String message = document.getString("message");
                            String timestamp = document.getString("timestamp");
                            String location = document.getString("location");
                            String imgUrl = document.getString("imgUrl");

                            Long createdAt = document.getLong("createdAt");

                            if (title != null && !title.isEmpty()) {
                                Notification item = new Notification(title, message, timestamp, location, imgUrl);
                                items.add(item);
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing document: " + e.getMessage());
                        }
                    }
                    listener.onNotificationLoaded(items);
                })
                .addOnFailureListener(listener::onError);
    }
}