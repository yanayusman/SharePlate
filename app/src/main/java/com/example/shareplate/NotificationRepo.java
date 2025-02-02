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
    private static final String COLLECTION_NAME = "notifications";
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserEmail = currentUser.getEmail();

        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> items = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String title = document.getString("itemName");
                            String message = document.getString("message");
                            Long timestamp = document.contains("timestamp") ? document.getLong("timestamp") : System.currentTimeMillis();
                            String location = document.getString("location");
                            String imgUrl = document.getString("imageUrl");
                            String ownerEmail = document.getString("ownerEmail");
                            String requesterEmail = document.getString("requesterEmail");
                            String activityType = document.getString("activityType");
                            String expiredDate = document.getString("expiredDate");
                            String notiType = document.getString("notiType");

                            // Add notifications for both owner and requester
                                // Add notifications for all users if notiType is "all"
                                if ("all".equals(notiType)) {
                                    Log.d("NotificationDebug", "Notification Type: " + title);
                                    Notification item = new Notification(title, message, timestamp, location, imgUrl, ownerEmail, requesterEmail, activityType, expiredDate, notiType);
                                    items.add(item);
                                } else if ("request".equals(notiType)) {
                                    Log.d("NotificationDebug", "Notification Type: " + title);
                                    Notification item = new Notification(title, message, timestamp, location, imgUrl, ownerEmail, requesterEmail, activityType, expiredDate, notiType);
                                    items.add(item);
                                } else if ((currentUserEmail.equals(ownerEmail) || currentUserEmail.equals(requesterEmail))) {
                                    // Add notifications for specific users based on email match
                                    Notification item = new Notification(title, message, timestamp, location, imgUrl, ownerEmail, requesterEmail, activityType, expiredDate, notiType);
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