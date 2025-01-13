package com.example.shareplate;

import java.util.Map;
import java.util.HashMap;
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
import java.util.Map;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import android.widget.EditText;
import android.text.InputType;
import android.view.Gravity;
import android.net.Uri;

public class FoodItemDetailFragment extends Fragment {
    public static final String ARG_DONATION_ITEM = "donation_item";

    private BroadcastReceiver profileUpdateReceiver;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DonationItem currentDonationItem;
    private String ownerUsername;
    private RecyclerView detailRecyclerView;

    public static FoodItemDetailFragment newInstance(DonationItem item) {
        FoodItemDetailFragment fragment = new FoodItemDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DONATION_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_food_item_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the DonationItem from arguments once
        if (getArguments() != null) {
            currentDonationItem = (DonationItem) getArguments().getSerializable(ARG_DONATION_ITEM);
        }
        if (currentDonationItem == null) {
            return;
        }

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.button_green,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        swipeRefreshLayout.setOnRefreshListener(this::refreshFoodDetails);

        // Set up back button in toolbar
        ImageView backButton = view.findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        // Initialize RecyclerView
        detailRecyclerView = view.findViewById(R.id.detail_recycler_view);
        detailRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Convert your existing layout content into a RecyclerView item
        View contentView = LayoutInflater.from(requireContext())
                .inflate(R.layout.food_item_detail_content, null);

        // Setup views and load data
        setupViews(contentView);

        // Create a simple adapter with single item
        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.food_item_detail_content, parent, false);
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
        updateUIWithDonationItem(view, currentDonationItem);

        Button editButton = view.findViewById(R.id.editButton);
        editButton.setVisibility(View.GONE); // Default to hidden

        // Show the edit button only if the current user is the owner
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentDonationItem != null) {
            String currentEmail = currentUser.getEmail();
            if (currentEmail != null && currentEmail.equals(currentDonationItem.getEmail())) {
                editButton.setVisibility(View.VISIBLE);
                editButton.setOnClickListener(v -> openEditFragment());
            } else {
                Log.d("EditButton", "Current user is not the owner.");
            }
        } else {
            Log.d("EditButton", "No authenticated user or donation item is null.");
        }
    }

    private void refreshFoodDetails() {
        if (currentDonationItem == null || currentDonationItem.getDocumentId() == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Get fresh data from Firestore
        FirebaseFirestore.getInstance()
                .collection("allDonationItems")
                .document(currentDonationItem.getDocumentId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            // Create new DonationItem from the fresh data
                            DonationItem refreshedItem = documentSnapshot.toObject(DonationItem.class);
                            if (refreshedItem != null) {
                                refreshedItem.setDocumentId(documentSnapshot.getId());
                                String feedback = documentSnapshot.getString("feedback");
                                String receiverEmail = documentSnapshot.getString("receiverEmail");
                                refreshedItem.setFeedback(feedback);
                                refreshedItem.setReceiverEmail(receiverEmail);
                                currentDonationItem = refreshedItem;

                                // Update UI with fresh data
                                if (getView() != null) {
                                    updateUIWithDonationItem(getView(), refreshedItem);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("FoodItemDetail", "Error refreshing data", e);
                            Toast.makeText(getContext(),
                                    "Error refreshing data: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("FoodItemDetail", "Failed to refresh", e);
                    Toast.makeText(getContext(),
                            "Failed to refresh: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void updateUIWithDonationItem(View view, DonationItem item) {
        ImageView itemImage = view.findViewById(R.id.detail_item_image);
        TextView itemName = view.findViewById(R.id.detail_item_name);
        TextView itemFoodCategory = view.findViewById(R.id.detail_item_category);
        TextView itemExpiredDate = view.findViewById(R.id.detail_item_expired_date);
        TextView itemQuantity = view.findViewById(R.id.detail_item_quantity);
        TextView itemPickupTime = view.findViewById(R.id.detail_item_pickup_time);
        TextView itemLocation = view.findViewById(R.id.detail_item_location);
        TextView itemOwner = view.findViewById(R.id.detail_item_owner);
        TextView itemStatus = view.findViewById(R.id.detail_item_status);
        TextView itemCreatedAt = view.findViewById(R.id.detail_item_created_at);
        ImageView ownerProfileImage = view.findViewById(R.id.owner_profile_image);
        TextView feedbackTitle = view.findViewById(R.id.feedback_section_title);
        TextView feedbackAuthor = view.findViewById(R.id.feedback_author);
        TextView feedbackContent = view.findViewById(R.id.feedback_content);

        // Add click listener to the image
        itemImage.setOnClickListener(v -> {
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

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(item.getEmail())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ownerUsername = documentSnapshot.getString("username");
                        itemOwner.setText(ownerUsername != null ? ownerUsername : "Anonymous");
                    } else {
                        itemOwner.setText("Anonymous");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching owner username: " + e.getMessage());
                    itemOwner.setText("Anonymous");
                });

        // Load the owner's profile image
        loadOwnerProfileImage(ownerUsername, ownerProfileImage);

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
                    .into(itemImage);
        } else {
            itemImage.setImageResource(item.getImageResourceId());
        }

        // Set text fields
        itemName.setText(item.getName());
        itemFoodCategory.setText("Food Category : " + (item.getFoodCategory() != null ? item.getFoodCategory() : "N/A"));
        itemExpiredDate.setText("Expires : " + (item.getExpiredDate() != null ? item.getExpiredDate() : "N/A"));
        itemQuantity.setText("Quantity : " + (item.getQuantity() != null ? item.getQuantity() : "N/A"));
        itemPickupTime.setText("Pickup Time : " + (item.getPickupTime() != null ? item.getPickupTime() : "N/A"));
        itemLocation.setText("Location : " + (item.getLocation() != null ? item.getLocation() : "N/A"));
        itemOwner.setText(ownerUsername != null ? ownerUsername : "Anonymous");

        // Show status if completed
        if ("completed".equals(item.getStatus())) {
            itemStatus.setVisibility(View.VISIBLE);
            itemStatus.setText("Status : Completed");
            itemStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            itemStatus.setVisibility(View.GONE);
        }

        // Show creation date
        itemCreatedAt.setText("Posted on " + item.getFormattedCreationDate());

        // Always check and update feedback visibility
        if (item != null && item.getFeedback() != null && !item.getFeedback().trim().isEmpty()) {
            feedbackTitle.setVisibility(View.VISIBLE);
            feedbackAuthor.setVisibility(View.VISIBLE);
            feedbackContent.setVisibility(View.VISIBLE);
            feedbackContent.setText(item.getFeedback());
            
            // Get and display receiver's username
            if (item.getReceiverEmail() != null) {
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(item.getReceiverEmail())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String receiverUsername = documentSnapshot.getString("username");
                                feedbackAuthor.setText("From: " + (receiverUsername != null ? receiverUsername : "Anonymous"));
                            } else {
                                feedbackAuthor.setText("From: Anonymous");
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Feedback", "Error fetching receiver username", e);
                            feedbackAuthor.setText("From: Anonymous");
                        });
            }
            Log.d("Feedback", "Showing feedback: " + item.getFeedback());
        } else {
            feedbackTitle.setVisibility(View.GONE);
            feedbackAuthor.setVisibility(View.GONE);
            feedbackContent.setVisibility(View.GONE);
            Log.d("Feedback", "No feedback to show");
        }

        // Update buttons visibility based on ownership and status
        updateButtonsVisibility(view, item);
        updateFeedbackButtonVisibility(view, item);

        // Add click listener to location text
        itemLocation.setOnClickListener(v -> {
            openLocationInMaps(item.getLocation());
        });
        
        // Make it look clickable
        itemLocation.setTextColor(getResources().getColor(R.color.button_green));
        itemLocation.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_location, 0);
        itemLocation.setPadding(0, 0, 8, 0);
    }

    private void updateButtonsVisibility(View view, DonationItem item) {
        Button deleteButton = view.findViewById(R.id.deleteButton);
        Button requestButton = view.findViewById(R.id.requestButton);
        Button completeButton = view.findViewById(R.id.completeButton);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Reset button visibility
        deleteButton.setVisibility(View.GONE);
        requestButton.setVisibility(View.GONE);
        completeButton.setVisibility(View.GONE);

        if (currentUser != null) {
            String currentEmail = currentUser.getEmail();
            if (currentEmail != null) {
                if (currentEmail.equalsIgnoreCase(item.getEmail())) {
                    // User is the owner: Show delete and complete buttons
                    deleteButton.setVisibility(View.VISIBLE);
                    deleteButton.setOnClickListener(v -> showDeleteConfirmation(item));

                    if ("active".equals(item.getStatus())) {
                        completeButton.setVisibility(View.VISIBLE);
                        completeButton.setOnClickListener(v -> showCompleteConfirmation(item));
                    }

                    // Hide request button for the owner
                    requestButton.setVisibility(View.GONE);
                } else {
                    // User is not the owner: Show request button if status is "active"
                    requestButton.setVisibility("active".equals(item.getStatus()) ? View.VISIBLE : View.GONE);
                    requestButton.setOnClickListener(v -> acceptRequestItem(item));
                }
            } else {
                Log.e("UpdateVisibility", "Current user's email is null.");
            }
        } else {
            Log.e("UpdateVisibility", "No authenticated user.");
        }
    }

    private void acceptRequestItem(DonationItem item) {
        if (item.getDocumentId() == null) {
            Toast.makeText(getContext(), "Error: Cannot update item without document ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in to request items", 
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new request document in the foodRequest collection
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("email", currentUser.getEmail());
        requestData.put("itemId", item.getDocumentId());
        requestData.put("timestamp", System.currentTimeMillis());
        
        db.collection("foodRequest")
            .add(requestData)
            .addOnSuccessListener(documentReference -> {
                // After adding to foodRequest, update the original item's status
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "completed");
                updates.put("receiverEmail", currentUser.getEmail());

                DonationItemRepository repository = new DonationItemRepository();
                repository.updateDonationWithFields(item.getDocumentId(), updates,
                        new DonationItemRepository.OnStatusUpdateListener() {
                            @Override
                            public void onUpdateSuccess() {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Request accepted successfully", Toast.LENGTH_SHORT).show();
                                    refreshFoodDetails();

                                    storeNotificationForOwner(item, currentUser.getEmail());

                                    // Broadcast the update
                                    Intent refreshIntent = new Intent("profile.stats.updated"); LocalBroadcastManager.getInstance(requireContext())
                                            .sendBroadcast(refreshIntent);
                                }
                            }

                            @Override
                            public void onUpdateFailure(Exception e) {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(),
                                            "Failed to accept request: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to create request: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            });
    }

    private void storeNotificationForOwner(DonationItem item, String requesterEmail) {
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
                    notificationData.put("message", requesterUsername + " has requested your donation!");

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the broadcast receiver
        profileUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String newProfileImageUrl = intent.getStringExtra("newProfileImageUrl");
                String ownerEmail = intent.getStringExtra("email");

                // Get the current donation item
                DonationItem currentItem = getArguments() != null ?
                        (DonationItem) getArguments().getSerializable(ARG_DONATION_ITEM) : null;

                // Update the profile image if this detail view is for the updated user's donation
                if (currentItem != null && currentItem.getEmail().equals(ownerEmail)) {
                    ImageView ownerProfileImage = getView().findViewById(R.id.owner_profile_image);
                    if (getContext() != null && ownerProfileImage != null) {
                        Glide.with(getContext())
                                .load(newProfileImageUrl)
                                .circleCrop()
                                .placeholder(R.drawable.profile)
                                .error(R.drawable.profile)
                                .into(ownerProfileImage);
                    }
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register the broadcast receiver
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity())
                    .registerReceiver(profileUpdateReceiver, new IntentFilter("profile.image.updated"));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the broadcast receiver
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity())
                    .unregisterReceiver(profileUpdateReceiver);
        }
    }

    private void showDeleteConfirmation(DonationItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Donation")
                .setMessage("Are you sure you want to delete this donation?")
                .setPositiveButton("Delete", (dialog, which) -> deleteDonationItem(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDonationItem(DonationItem item) {
        if (item.getDocumentId() == null) {
            Toast.makeText(getContext(), "Error: Cannot delete item without document ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        System.out.println("Deleting item with document ID: " + item.getDocumentId());

        DonationItemRepository repository = new DonationItemRepository();
        repository.deleteDonationItem(item.getDocumentId(), new DonationItemRepository.OnDeleteCompleteListener() {
            @Override
            public void onDeleteSuccess() {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Donation deleted successfully", Toast.LENGTH_SHORT).show();
                    // Navigate back
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            }

            @Override
            public void onDeleteFailure(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to delete donation: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showCompleteConfirmation(DonationItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Complete Donation")
                .setMessage("Mark this donation as completed?")
                .setPositiveButton("Complete", (dialog, which) -> completeDonation(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void completeDonation(DonationItem item) {
        if (item.getDocumentId() == null) {
            Toast.makeText(getContext(), "Error: Cannot update item without document ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        DonationItemRepository repository = new DonationItemRepository();
        repository.updateDonationStatus(item.getDocumentId(), "completed",
                new DonationItemRepository.OnStatusUpdateListener() {
                    @Override
                    public void onUpdateSuccess() {
                        if (getContext() != null) {
                            // Update the UI to show completed status
                            TextView itemStatus = getView().findViewById(R.id.detail_item_status);
                            itemStatus.setVisibility(View.VISIBLE);
                            itemStatus.setText("Status: Completed");
                            itemStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                            // Hide the complete button
                            Button completeButton = getView().findViewById(R.id.completeButton);
                            completeButton.setVisibility(View.GONE);

                            // Hide the request button
                            Button requestButton = getView().findViewById(R.id.requestButton);
                            requestButton.setVisibility(View.GONE);

                            Toast.makeText(getContext(),
                                    "Donation marked as complete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onUpdateFailure(Exception e) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "Failed to update donation: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadOwnerProfileImage(String ownerUsername, ImageView ownerProfileImage) {
        // Get the DonationItem from arguments
        if (getArguments() != null) {
            DonationItem donationItem = (DonationItem) getArguments().getSerializable(ARG_DONATION_ITEM);
            if (donationItem != null && donationItem.getOwnerProfileImageUrl() != null
                    && !donationItem.getOwnerProfileImageUrl().isEmpty()) {
                // Load the profile image using the stored URL
                if (getContext() != null) {
                    Glide.with(getContext())
                            .load(donationItem.getOwnerProfileImageUrl())
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

    private void openEditFragment() {
        EditDonationFragment editFragment = EditDonationFragment.newInstance(currentDonationItem);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    private void updateFeedbackButtonVisibility(View view, DonationItem item) {
        Button feedbackButton = view.findViewById(R.id.feedbackButton);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser != null && item != null && "completed".equals(item.getStatus())) {
            // Show feedback button only for the receiver and if no feedback exists yet
            if (currentUser.getEmail().equals(item.getReceiverEmail()) && 
                (item.getFeedback() == null || item.getFeedback().isEmpty())) {
                feedbackButton.setVisibility(View.VISIBLE);
                feedbackButton.setOnClickListener(v -> showFeedbackDialog(item));
            } else {
                feedbackButton.setVisibility(View.GONE);
            }
        } else {
            feedbackButton.setVisibility(View.GONE);
        }
    }

    private void showFeedbackDialog(DonationItem item) {
        EditText feedbackInput = new EditText(requireContext());
        feedbackInput.setHint("Enter your feedback");
        feedbackInput.setMinLines(3);
        feedbackInput.setGravity(Gravity.TOP | Gravity.START);
        feedbackInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Leave Feedback")
                .setView(feedbackInput)
                .setPositiveButton("Submit", (dialogInterface, i) -> {
                    String feedback = feedbackInput.getText().toString().trim();
                    if (!feedback.isEmpty()) {
                        submitFeedback(item, feedback);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private void submitFeedback(DonationItem item, String feedback) {
        FirebaseFirestore.getInstance()
                .collection("allDonationItems")
                .document(item.getDocumentId())
                .update("feedback", feedback)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Feedback submitted successfully", Toast.LENGTH_SHORT).show();
                    // Update the current item with the new feedback
                    item.setFeedback(feedback);
                    // Update the UI
                    View view = getView();
                    if (view != null) {
                        TextView feedbackTitle = view.findViewById(R.id.feedback_section_title);
                        TextView feedbackAuthor = view.findViewById(R.id.feedback_author);
                        TextView feedbackContent = view.findViewById(R.id.feedback_content);
                        feedbackTitle.setVisibility(View.VISIBLE);
                        feedbackAuthor.setVisibility(View.VISIBLE);
                        feedbackContent.setVisibility(View.VISIBLE);
                        feedbackContent.setText(feedback);
                        
                        // Get current user's username
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(currentUser.getEmail())
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String username = documentSnapshot.getString("username");
                                            feedbackAuthor.setText("From: " + (username != null ? username : "Anonymous"));
                                        }
                                    });
                        }
                    }
                    // Hide the feedback button
                    Button feedbackButton = getView().findViewById(R.id.feedbackButton);
                    feedbackButton.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to submit feedback: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void openLocationInMaps(String location) {
        try {
            // Create a Uri from the location string
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
            
            // Create an Intent from gmmIntentUri
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            
            // Make the intent explicit by setting Google Maps package
            mapIntent.setPackage("com.google.android.apps.maps");
            
            // Verify that the intent will resolve to an activity
            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // If Google Maps app is not installed, open in browser
                Uri browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(location));
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
                startActivity(browserIntent);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error opening maps: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Consolidated method for handling notifications
    private void handleNotification(String type, String title, String message, String recipientEmail) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("type", type);
        notification.put("read", false);
        notification.put("recipientEmail", recipientEmail);

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> 
                    Log.d("Notification", "Notification stored successfully"))
                .addOnFailureListener(e -> 
                    Log.e("Notification", "Error storing notification", e));
    }

    // Consolidated method for handling item status updates
    private void updateItemStatus(String newStatus, String message, OnStatusUpdateListener listener) {
        if (currentDonationItem == null || currentDonationItem.getDocumentId() == null) {
            if (listener != null) {
                listener.onUpdateFailure(new Exception("Invalid donation item"));
            }
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        
        FirebaseFirestore.getInstance()
                .collection("allDonationItems")
                .document(currentDonationItem.getDocumentId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) {
                        listener.onUpdateSuccess();
                    }
                    // Update UI
                    currentDonationItem.setStatus(newStatus);
                    refreshFoodDetails();
                    // Send notification
                    handleNotification(
                        "status_update",
                        "Item Status Updated",
                        message,
                        currentDonationItem.getEmail()
                    );
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onUpdateFailure(e);
                    }
                });
    }

    // Interface for status update callbacks
    private interface OnStatusUpdateListener {
        void onUpdateSuccess();
        void onUpdateFailure(Exception e);
    }

    // Consolidated method for handling button visibility
    private void updateButtonVisibility(View view, DonationItem item) {
        if (item == null) return;

        Button editButton = view.findViewById(R.id.editButton);
        Button deleteButton = view.findViewById(R.id.deleteButton);
        Button completeButton = view.findViewById(R.id.completeButton);
        Button feedbackButton = view.findViewById(R.id.feedbackButton);
        Button requestButton = view.findViewById(R.id.requestButton);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner = currentUser != null && 
                         currentUser.getEmail() != null && 
                         currentUser.getEmail().equals(item.getEmail());
        boolean isPending = "Pending".equals(item.getStatus());
        boolean isAccepted = "Accepted".equals(item.getStatus());
        boolean isCompleted = "Completed".equals(item.getStatus());

        requestButton.setVisibility(isPending && !isOwner ? View.VISIBLE : View.GONE);
        editButton.setVisibility(isPending && isOwner ? View.VISIBLE : View.GONE);
        deleteButton.setVisibility(isPending && isOwner ? View.VISIBLE : View.GONE);
        completeButton.setVisibility(isAccepted && isOwner ? View.VISIBLE : View.GONE);
        feedbackButton.setVisibility(isCompleted && !isOwner ? View.VISIBLE : View.GONE);
    }
} 