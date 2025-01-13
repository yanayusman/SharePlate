package com.example.shareplate;

import android.content.BroadcastReceiver;
import android.graphics.Outline;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// This fragment displays the list of all notifications in the app.
// It handles the display of all notifications by using a RecyclerView to show each notification item.

public class NotificationFragment extends Fragment {
    private static final String ARG_NOTIFICATION = "notification";
    private Notification notification;
    private RecyclerView detailRecyclerView;

    public static NotificationFragment newInstance(Notification item) {
        NotificationFragment fragment = new NotificationFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_NOTIFICATION, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            notification = (Notification) getArguments().getSerializable(ARG_NOTIFICATION);
        }
        if (notification == null) {
            return;
        }

        ImageView backButton = view.findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        detailRecyclerView = view.findViewById(R.id.notificationRecyclerView);
        detailRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        View contentView = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_notification_details, null);

        setupViews(contentView);

        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fragment_notification_details, parent, false);
                return new RecyclerView.ViewHolder(itemView) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                setupViews(holder.itemView);
            }

            @Override
            public int getItemCount() {
                return 1;
            }
        };

        detailRecyclerView.setAdapter(adapter);
    }

    private void setupViews(View view) {
        // Remove the back button setup since it's now handled in onViewCreated
        // Get views and set their values
        ImageView notificationImg = view.findViewById(R.id.notificationImage);
        TextView notificationTitle = view.findViewById(R.id.notificationTitle);
        TextView notificationDate = view.findViewById(R.id.notificationTimestamp);
        TextView notificationMessage = view.findViewById(R.id.notificationMessage);
        TextView notificationLocation = view.findViewById(R.id.notificationDetails);

        notificationImg.setOnClickListener(v -> {
            FullScreenImageFragment fullScreenFragment =
                    FullScreenImageFragment.newInstance(notification.getImgUrl(), 0);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fullScreenFragment)
                    .addToBackStack(null)
                    .commit();
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserEmail = currentUser.getEmail();

        // Handle expiration notifications
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        if(!"event".equals(notification.getActivityType())){
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
                        return;
                    }
                }
            } catch (ParseException e) {
                Log.e("ExpirationAlert", "Failed to parse expiration date: " + notification.getExpiredDate(), e);
            }
        }

        if (currentUserEmail != null) {
            if ("event".equals(notification.getActivityType())) {
                if ("all".equals(notification.getNotiType())) {

                    imageUpload(notificationImg);

                    notificationTitle.setText("NEW EVENT!!\n " + notification.getTitle());
                    notificationMessage.setText("Description: " + (notification.getMessage() != null ? notification.getMessage() : "N/A"));
                    notificationDate.setText("Date: " + (notification.getExpiredDate() != null ? notification.getExpiredDate() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));

                } else if (currentUserEmail.equals(notification.getRequesterEmail())) {

                    imageUpload(notificationImg);

                    notificationTitle.setText("REMINDER!!\n " + notification.getTitle());
                    notificationMessage.setText("Dont forget Your Upcoming Event!!");
                    notificationDate.setText("Date and Time : " + (notification.getTimestamp() != null ? notification.getTimestamp() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                } else if (currentUserEmail.equals(notification.getOwnerEmail())) {

                    imageUpload(notificationImg);

                    notificationTitle.setText("New Participant to Help with Your Event!!\n " + notification.getTitle());
                    notificationMessage.setText("Description: " + (notification.getMessage() != null ? notification.getMessage() : "N/A"));
                    notificationDate.setText("Posted on: " + (notification.getTimestamp() != null ? notification.getTimestamp() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }
            } else if("request".equals(notification.getActivityType())){
                if("all".equals(notification.getNotiType())){

                    imageUpload(notificationImg);

                    notificationTitle.setText("NEW REQUEST!!\n " + notification.getTitle());
                    notificationMessage.setText("Description: " + (notification.getMessage() != null ? notification.getMessage() : "N/A"));
                    notificationDate.setText("Posted on: " + (notification.getExpiredDate() != null ? notification.getExpiredDate() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }else if(currentUserEmail.equals(notification.getRequesterEmail())){

                    imageUpload(notificationImg);

                    notificationTitle.setText("Donation Processing...\n " + notification.getTitle());
                    notificationMessage.setText("Description: Your donation has been successfully processed.");
                    notificationDate.setText("Posted on: " + (notification.getTimestamp() != null ? notification.getTimestamp() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }else if(currentUserEmail.equals(notification.getOwnerEmail())){

                    imageUpload(notificationImg);

                    notificationTitle.setText("Successfull Request!!\n " + notification.getTitle());
                    notificationMessage.setText("Description: Your request has been processed. All you have to do is wait at your door step;) ");
                    notificationDate.setText("Posted on: " + (notification.getTimestamp() != null ? notification.getTimestamp() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }
            } else {
                if("all".equals(notification.getNotiType())){

                    imageUpload(notificationImg);

                    notificationTitle.setText("NEW DONATION!!\n " + notification.getTitle());
                    notificationMessage.setText("Description: " + (notification.getMessage() != null ? notification.getMessage() : "N/A"));
                    notificationDate.setText("Posted on: " + (notification.getExpiredDate() != null ? notification.getExpiredDate() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }else if(currentUserEmail.equals(notification.getOwnerEmail())){

                    imageUpload(notificationImg);

                    notificationTitle.setText("New Request For Your Donation!!\n " + notification.getTitle());
                    notificationMessage.setText("Description: " + (notification.getMessage() != null ? notification.getMessage() : "N/A"));
                    notificationDate.setText("Posted on: " + (notification.getTimestamp() != null ? notification.getTimestamp() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }else if(currentUserEmail.equals(notification.getRequesterEmail())) {

                    imageUpload(notificationImg);

                    notificationTitle.setText("Request Proccessing!!\n " + notification.getTitle());
                    notificationMessage.setText("Your request has been successfully processed. All you have to do is wait at your door step;)");
                    notificationDate.setText("Posted on: " + (notification.getTimestamp() != null ? notification.getTimestamp() : "N/A"));
                    notificationLocation.setText("Location: " + (notification.getLocation() != null ? notification.getLocation() : "N/A"));
                }
            }
        }
    }

    private void imageUpload(ImageView notificationImg){
        if (notification.getImgUrl() != null && !notification.getImgUrl().isEmpty()) {
            Glide.with(this)
                    .load(notification.getImgUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(notificationImg);
        } else {
            notificationImg.setImageResource(0);
        }
    }
}