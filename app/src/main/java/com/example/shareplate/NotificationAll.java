package com.example.shareplate;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class NotificationAll extends Fragment {

    private Toolbar toolbar;
    private ImageView searchIcon;
    private LinearLayout eventGrid;
    private EditText searchEditText;
    private List<Notification> allNoti = new ArrayList<>();
    private LinearLayout searchLayout;
    private ImageView backArrow;
    private View normalToolbarContent;
    private NotificationRepo notificationRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification_all, container, false);

        notificationRepo = new NotificationRepo();
        fetchEventsFromDatabase();

        // Initialize views
        toolbar = view.findViewById(R.id.toolbar3);
        eventGrid = view.findViewById(R.id.event_grid);
        backArrow = view.findViewById(R.id.back_arrow1);
        normalToolbarContent = view.findViewById(R.id.normal_toolbar_content2);
        searchLayout = view.findViewById(R.id.search_layout2);
        searchEditText = view.findViewById(R.id.search_edit_text2);

        if (backArrow != null) {
            backArrow.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }
        return view;
    }

    private void fetchEventsFromDatabase() {
        notificationRepo.getAllNotification(new NotificationRepo.OnNotificationLoadedListener() {
            @Override
            public void onNotificationLoaded(List<Notification> items) {
                if (isAdded() && getView() != null) {
                    for (Notification item : items) {
                        addNotificationView(item);
                    }
                } else {
                    Log.e("NotificationAll", "Fragment not attached, skipping UI update.");
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded() && getContext() != null) { // Ensure the Fragment is still active
                    Toast.makeText(getContext(), "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addNotificationView(Notification notification) {
        View eventItem = getLayoutInflater().inflate(R.layout.notification_view, eventGrid, false);
        ImageView notificationImg = eventItem.findViewById(R.id.item_image);
        TextView notificationTitle = eventItem.findViewById(R.id.item_name);
        TextView notificationDate = eventItem.findViewById(R.id.item_date);
        TextView notificationLocation = eventItem.findViewById(R.id.item_location);
        TextView notificationMessage = eventItem.findViewById(R.id.item_desc);

        // Load image
        if (notification.getImgUrl() != null && !notification.getImgUrl().isEmpty()) {
            Glide.with(this)
                    .load(notification.getImgUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(notificationImg);
        } else {
            notificationImg.setImageResource(R.drawable.placeholder_image);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserEmail = currentUser != null ? currentUser.getEmail() : null;

        // Handle expiration notifications
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        try {
            String expirationString = notification.getExpiredDate();
            if (expirationString != null && !expirationString.isEmpty()) {
                Date expirationDate = dateFormat.parse(expirationString);
                long expirationTimeMillis = expirationDate.getTime();

                // Get current date at midnight
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long currentDateMillis = calendar.getTimeInMillis();

                // Check if expiring within one day
                if (expirationTimeMillis > currentDateMillis && expirationTimeMillis - currentDateMillis <= 24 * 60 * 60 * 1000) {
                    notificationTitle.setText("[Alert] Food Expiring Soon: " + notification.getTitle());
                    notificationDate.setText("Expires on: " + expirationString);
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                    notificationMessage.setText("Description: " + (notification.getMessage() != null ? notification.getMessage() : "N/A"));
                    setupItemClick(eventItem, notification);
                    eventGrid.addView(eventItem);
                    return;
                }
            }
        } catch (ParseException e) {
            Log.e("ExpirationAlert", "Failed to parse expiration date: " + notification.getExpiredDate(), e);
        }


        // Handle other notifications
        if (currentUserEmail != null) {

            if("event".equals(notification.getActivityType())){
                if("all".equals(notification.getNotiType())){
                    notificationTitle.setText("[New Event] " + notification.getTitle());
                    notificationMessage.setText("Description: " + (notification.getMessage() != null ? notification.getMessage() : "N/A"));
                    notificationDate.setText("Posted on: " + (notification.getExpiredDate() != null ? notification.getExpiredDate() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }else if(currentUserEmail.equals(notification.getRequesterEmail())){
                    notificationTitle.setText("[Reminder] Your Upcoming Event!! " + notification.getTitle());
                    notificationMessage.setVisibility(View.GONE);
                    notificationDate.setText("Posted on: " + (notification.getTimestamp() != null ? notification.getTimestamp() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }else if(currentUserEmail.equals(notification.getOwnerEmail())){
                    notificationTitle.setText("[New Member] " + notification.getTitle());
                    notificationMessage.setText("Description: " + (notification.getMessage() != null ? notification.getMessage() : "N/A"));
                    notificationDate.setText("Posted on: " + (notification.getTimestamp() != null ? notification.getTimestamp() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }
            }else if("request".equals(notification.getActivityType())){
                if("all".equals(notification.getNotiType())){
                    notificationTitle.setText("[New Request] " + notification.getTitle());
                    notificationMessage.setText("Description: " + (notification.getMessage() != null ? notification.getMessage() : "N/A"));
                    notificationDate.setText("Posted on: " + (notification.getExpiredDate() != null ? notification.getExpiredDate() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }else if(currentUserEmail.equals(notification.getRequesterEmail())){
                    notificationTitle.setText("[Donation Processing...] " + notification.getTitle());
                    notificationMessage.setVisibility(View.GONE);
                    notificationDate.setText("Posted on: " + (notification.getTimestamp() != null ? notification.getTimestamp() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }else if(currentUserEmail.equals(notification.getOwnerEmail())){
                    notificationTitle.setText("[Successfull Request] " + notification.getTitle());
                    notificationMessage.setText("Description: " + (notification.getMessage() != null ? notification.getMessage() : "N/A"));
                    notificationDate.setText("Posted on: " + (notification.getTimestamp() != null ? notification.getTimestamp() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }
            }else {
                if("all".equals(notification.getNotiType())){
                    notificationTitle.setText("[New Donation] " + notification.getTitle());
                    notificationMessage.setText("Description: " + (notification.getMessage() != null ? notification.getMessage() : "N/A"));
                    notificationDate.setText("Posted on: " + (notification.getExpiredDate() != null ? notification.getExpiredDate() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }else if(currentUserEmail.equals(notification.getOwnerEmail())){
                    notificationTitle.setText("[Donation Request] " + notification.getTitle());
                    notificationMessage.setText("Description: " + (notification.getMessage() != null ? notification.getMessage() : "N/A"));
                    notificationDate.setText("Posted on: " + (notification.getTimestamp() != null ? notification.getTimestamp() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }else if(currentUserEmail.equals(notification.getRequesterEmail())) {
                    notificationTitle.setText("[Request Proccessing...] " + notification.getTitle());
                    notificationMessage.setVisibility(View.GONE);
                    notificationDate.setText("Posted on: " + (notification.getTimestamp() != null ? notification.getTimestamp() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }
            }
        }

        setupItemClick(eventItem, notification);

        // Check and remove if already in parent view
        if (eventItem.getParent() != null) {
            ((ViewGroup) eventItem.getParent()).removeView(eventItem);
        }
        eventGrid.addView(eventItem);
    }

    private void setupItemClick(View eventItem, Notification notification) {
        eventItem.setOnClickListener(v -> {
            NotificationFragment detailFragment = NotificationFragment.newInstance(notification);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

}
