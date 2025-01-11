package com.example.shareplate;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewOutlineProvider;
import android.graphics.Outline;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Log;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;

public class CommunityDetailsFragment extends Fragment {
    public static final String ARG_EVENT_ITEM = "event_item";
    private BroadcastReceiver profileUpdateReceiver;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Event currentEventItem;
    private RecyclerView detailRecyclerView;

    public static CommunityDetailsFragment newInstance(Event item) {
        CommunityDetailsFragment fragment = new CommunityDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            currentEventItem = (Event) getArguments().getSerializable(ARG_EVENT_ITEM);
        }
        if (currentEventItem == null) {
            return;
        }

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.button_green,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        swipeRefreshLayout.setOnRefreshListener(this::refreshEventDetails);

        ImageView backButton = view.findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        detailRecyclerView = view.findViewById(R.id.detail_recycler_view);
        detailRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        View contentView = LayoutInflater.from(requireContext())
                .inflate(R.layout.event_item_detail_content, null);

        setupViews(contentView);

        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.event_item_detail_content, parent, false);

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentEventItem = (Event) getArguments().getSerializable(ARG_EVENT_ITEM);
        }
    }

    private void setupViews(View view) {
        // Remove the back button setup since it's now handled in onViewCreated
        // Get views and set their values
        updateUIWithEventItem(view, currentEventItem);

        Button editButton = view.findViewById(R.id.editButton);
        Button deleteButton = view.findViewById(R.id.deleteButton);
        Button joinButton = view.findViewById(R.id.joinButton);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Reset button visibility
        deleteButton.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);
        joinButton.setVisibility(View.GONE);

        if (currentUser != null) {
            String currentEmail = currentUser.getEmail();
            if (currentEmail != null) {
                if (currentEmail.equalsIgnoreCase(currentEventItem.getEmail())) {
                    // User is the owner: Show delete and complete buttons
                    deleteButton.setVisibility(View.VISIBLE);
                    deleteButton.setOnClickListener(v -> showDeleteConfirmation(currentEventItem));

                    editButton.setVisibility(View.VISIBLE);
                    editButton.setOnClickListener(v -> openEditFragment());
                    // Hide request button for the owner
                    joinButton.setVisibility(View.GONE);
                } else {
                    // User is not the owner
                    updateButtonsVisibility(view, currentEventItem);
                }
            } else {
                Log.e("UpdateVisibility", "Current user's email is null.");
            }
        } else {
            Log.e("UpdateVisibility", "No authenticated user.");
        }
    }

    private void addEventToUser(View view, String ownerEmail) {
        // Firestore instance
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // Prepare event details
        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("eventName", currentEventItem.getName());
        eventDetails.put("eventType", currentEventItem.getTypeOfEvents());
        eventDetails.put("eventDate", currentEventItem.getDate());
        eventDetails.put("eventTime", currentEventItem.getTime());
        eventDetails.put("eventLocation", currentEventItem.getLocation());

        // Add event to user's subcollection
        firestore.collection("users")
                .document(currentUserEmail)
                .collection("joinedEvents")
                .add(eventDetails)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(view.getContext(), "Event joined successfully!", Toast.LENGTH_SHORT).show();

                    storeNotificationForOwner(currentEventItem, currentUserEmail);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(view.getContext(), "Failed to join event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void storeNotificationForOwner(Event item, String requesterEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch the username from the users collection
        db.collection("users")
                .whereEqualTo("email", requesterEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String requesterUsername = "Unknown User";
                    if (!querySnapshot.isEmpty()) {
                        requesterUsername = querySnapshot.getDocuments().get(0).getString("username");
                    }

                    Map<String, Object> notificationData = new HashMap<>();
                    notificationData.put("ownerEmail", item.getEmail());
                    notificationData.put("itemId", item.getDocumentId());
                    notificationData.put("itemName", item.getName());
                    notificationData.put("requesterEmail", requesterEmail);
                    notificationData.put("location", item.getLocation());
                    notificationData.put("imageUrl", item.getImageUrl());
                    notificationData.put("timestamp", System.currentTimeMillis());
                    notificationData.put("status", "unread");
                    notificationData.put("message", requesterUsername + " has joined your event!");

                    db.collection("notifications")
                            .add(notificationData)
                            .addOnSuccessListener(documentReference -> {
                                Log.d("Notification", "Notification stored successfully");
                            })
                            .addOnFailureListener(e -> {
                                Log.e("NotificationError", "Failed to store notification: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("UserQueryError", "Failed to fetch requester username: " + e.getMessage());

                    // Fallback: Store notification with requester email if username fetch fails
                    Map<String, Object> notificationData = new HashMap<>();
                    notificationData.put("ownerEmail", item.getEmail());
                    notificationData.put("itemId", item.getDocumentId());
                    notificationData.put("itemName", item.getName());
                    notificationData.put("requesterEmail", requesterEmail);
                    notificationData.put("location", item.getLocation());
                    notificationData.put("imageUrl", item.getImageUrl());
                    notificationData.put("timestamp", System.currentTimeMillis());
                    notificationData.put("status", "unread");
                    notificationData.put("message", requesterEmail + " has joined your event!");
                    notificationData.put("activityType", "event");

                    db.collection("notifications")
                            .add(notificationData)
                            .addOnSuccessListener(documentReference -> {
                                Log.d("Notification", "Notification stored successfully (fallback to email)");
                            })
                            .addOnFailureListener(err -> {
                                Log.e("NotificationError", "Failed to store notification (fallback): " + err.getMessage());
                            });
                });
    }

    private void refreshEventDetails() {
        if (currentEventItem == null || currentEventItem.getDocumentId() == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(currentEventItem.getDocumentId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            Event refreshedItem = documentSnapshot.toObject(Event.class);
                            if (refreshedItem != null) {
                                refreshedItem.setDocumentId(documentSnapshot.getId());
                                currentEventItem = refreshedItem;

                                if (getView() != null) {
                                    updateUIWithEventItem(getView(), refreshedItem);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("EventDetail", "Error refreshing data", e);
                            Toast.makeText(getContext(),
                                    "Error refreshing data: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("EventDetail", "Failed to refresh", e);
                    Toast.makeText(getContext(),
                            "Failed to refresh: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void updateUIWithEventItem(View view, Event item) {
        ImageView eventImage = view.findViewById(R.id.detail_event_image);
        TextView eventName = view.findViewById(R.id.detail_event_name);
        TextView eventType = view.findViewById(R.id.detail_event_type);
        TextView eventDate = view.findViewById(R.id.detail_event_date);
        TextView eventTime = view.findViewById(R.id.detail_event_time);
        TextView eventSeatsAvailable = view.findViewById(R.id.detail_event_seats_available);
        TextView eventLocation = view.findViewById(R.id.detail_event_location);
        TextView eventCreatedAt = view.findViewById(R.id.detail_event_created_at);
        TextView eventDescription = view.findViewById(R.id.detail_event_desc);
        TextView ownerUsername = view.findViewById(R.id.detail_item_owner);
        ImageView ownerProfileImage = view.findViewById(R.id.owner_profile_image);

        eventImage.setOnClickListener(v -> {
            FullScreenImageFragment fullScreenFragment =
                    FullScreenImageFragment.newInstance(item.getImageUrl(), item.getImageResourceId());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fullScreenFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Set up owner profile image
        ownerProfileImage.setClipToOutline(true);
        ownerProfileImage.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });

        // Load the owner's profile image
        loadOwnerProfileImage(item.getOwnerUsername(), ownerProfileImage);

        // Set up owner profile image click listener
        ownerProfileImage.setOnClickListener(v -> {
            // Create and show FullScreenImageFragment with the owner's profile image
            FullScreenImageFragment fullScreenFragment =
                    FullScreenImageFragment.newInstance(item.getOwnerProfileImageUrl(), R.drawable.profile);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fullScreenFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Load item image
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(eventImage);
        } else {
            eventImage.setImageResource(item.getImageResourceId());
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("email", item.getEmail())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        String username = document.getString("username");
                        ownerUsername.setText(username);
                        Log.d("Firestore", "Fetched username: " + username);
                })
                .addOnFailureListener(e -> {
                    // Handle errors
                    Log.e("Firestore", "Error fetching username: ", e);
                });
        eventName.setText(item.getName());
        eventType.setText("Type Of Events : " + (item.getTypeOfEvents() != null ? item.getTypeOfEvents() : "N/A"));
        eventDate.setText("Date : " + (item.getDate() != null ? item.getDate() : "N/A"));
        eventTime.setText("Time : " + (item.getTime() != null ? item.getTime() : "N/A"));
        eventSeatsAvailable.setText("Seats Available : " + (item.getSeatAvailable() != null ? item.getSeatAvailable() : "N/A"));
        eventDescription.setText("Description : " + (item.getDescription() != null ? item.getDescription() : "N/A"));
        eventLocation.setText((item.getLocation() != null ? item.getLocation() : "N/A"));

        eventCreatedAt.setText("Posted on " + item.getFormattedCreationDate());
    }

    private void updateButtonsVisibility(View view, Event item) {
        Button joinButton = view.findViewById(R.id.joinButton);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String currentEmail = currentUser.getEmail();
            if (currentEmail != null) {
                // Fetch the user's ownerUsername from Firestore
                firestore.collection("users")
                        .document(currentEmail)
                        .collection("joinedEvents")
                        .whereEqualTo("eventName", currentEventItem.getName())
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                // User has already joined the event
                                joinButton.setVisibility(View.VISIBLE);
                                joinButton.setEnabled(false);
                                joinButton.setText("Joined!");
                                joinButton.setBackgroundColor(Color.LTGRAY);
                                joinButton.setTextColor(Color.WHITE);
                            } else {
                                // User has not joined the event
                                joinButton.setVisibility(View.VISIBLE);
                                joinButton.setEnabled(true);
                                joinButton.setText("Join Now!");
                                joinButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.button_green_dark));
                                joinButton.setOnClickListener(v -> addEventToUser(view, item.getEmail()));
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ButtonsVisibility", "Failed to check joined events: " + e.getMessage());
                            joinButton.setVisibility(View.GONE);
                        });
            }
        }
    }

    private void showDeleteConfirmation(Event item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Donation")
                .setMessage("Are you sure you want to delete this donation?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEventItem(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEventItem(Event item) {
        if (item.getDocumentId() == null) {
            Toast.makeText(getContext(), "Error: Cannot delete item without document ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        System.out.println("Deleting item with document ID: " + item.getDocumentId());

        EventRepo repository = new EventRepo();
        repository.deleteEvent(item.getDocumentId(), new EventRepo.OnDeleteCompleteListener() {
            @Override
            public void onDeleteSuccess() {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Event deleted successfully", Toast.LENGTH_SHORT).show();
                    // Navigate back
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            }

            @Override
            public void onDeleteFailure(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to delete event: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchUsernameFromFirestore(String email, View view) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users") // Replace with your Firestore collection name
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String username = querySnapshot.getDocuments().get(0).getString("username");
                        TextView usernameTextView = view.findViewById(R.id.detail_item_owner);
                        if (username != null) {
                            usernameTextView.setText(username);
                        } else {
                            usernameTextView.setText("Username not found");
                        }
                    } else {
                        Log.d("Firestore", "No matching user found for the given email");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching username: ", e);
                });
    }

    private void openEditFragment() {
        EditEventFragment editFragment = EditEventFragment.newInstance(currentEventItem);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadOwnerProfileImage(String ownerUsername, ImageView ownerProfileImage) {
        // Get the DonationItem from arguments
        if (getArguments() != null) {
            Event event = (Event) getArguments().getSerializable(ARG_EVENT_ITEM);
            if (event != null && event.getOwnerProfileImageUrl() != null
                    && !event.getOwnerProfileImageUrl().isEmpty()) {
                // Load the profile image using the stored URL
                if (getContext() != null) {
                    Glide.with(getContext())
                            .load(event.getOwnerProfileImageUrl())
                            .circleCrop()
                            .placeholder(R.drawable.profile)
                            .error(R.drawable.profile)
                            .into(ownerProfileImage);
                }
            } else {
                // Load default image if no URL available
                if (getContext() != null) {
                    Glide.with(getContext())
                            .load(R.drawable.profile)
                            .circleCrop()
                            .into(ownerProfileImage);
                }
            }
        }
    }

}
