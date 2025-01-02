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

import java.util.ArrayList;
import java.util.List;

// This fragment displays the list of all notifications in the app.
// It handles the display of all notifications by using a RecyclerView to show each notification item.

public class NotificationFragment extends Fragment {
    private static final String ARG_NOTIFICATION = "notification";
    private BroadcastReceiver profileUpdateReceiver;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Notification notification;
    private RecyclerView detailRecyclerView;
    private RecyclerView notificationRecyclerView;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;

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
        ImageView notiImg = view.findViewById(R.id.notificationImage);
        TextView notiTitle = view.findViewById(R.id.notificationTitle);
        TextView notiTimestamp = view.findViewById(R.id.notificationTimestamp);
        TextView notiMessage = view.findViewById(R.id.notificationMessage);
        TextView notiLocation = view.findViewById(R.id.notificationDetails);

        notiImg.setOnClickListener(v -> {
            FullScreenImageFragment fullScreenFragment =
                    FullScreenImageFragment.newInstance(notification.getImgUrl(), 0);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fullScreenFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Load item image
        if (notification.getImgUrl() != null && !notification.getImgUrl().isEmpty()) {
            Glide.with(this)
                    .load(notification.getImgUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(notiImg);
        } else {
            notiImg.setImageResource(0);
        }

        notiTitle.setText(notification.getTitle());
        notiTimestamp.setText("Posted on : " + (notification.getTimestamp()));
        notiMessage.setText(notification.getMessage());
        notiLocation.setText("Location: " +(notification.getLocation()));

    }
}