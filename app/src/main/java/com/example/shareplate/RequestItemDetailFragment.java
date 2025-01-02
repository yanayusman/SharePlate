package com.example.shareplate;

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

public class RequestItemDetailFragment extends Fragment {
    private static final String ARG_FOOD_ITEM = "food_item";
    private String ownerUsername;
    private BroadcastReceiver profileUpdateReceiver;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RequestFood currentRequestItem;
    private RecyclerView detailRecyclerView;

    public static RequestItemDetailFragment newInstance(RequestFood item) {
        RequestItemDetailFragment fragment = new RequestItemDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FOOD_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_request_item_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the DonationItem from arguments once
        if (getArguments() != null) {
            currentRequestItem = (RequestFood) getArguments().getSerializable(ARG_FOOD_ITEM);
        }
        if (currentRequestItem == null) {
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
        updateUIWithRequestItem(view, currentRequestItem);
    }

    private void refreshFoodDetails() {
        if (currentRequestItem == null || currentRequestItem.getDocumentId() == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Get fresh data from Firestore
        FirebaseFirestore.getInstance()
                .collection("foodRequest")
                .document(currentRequestItem.getDocumentId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            // Create new RequestFood from the fresh data
                            RequestFood refreshedItem = documentSnapshot.toObject(RequestFood.class);
                            if (refreshedItem != null) {
                                refreshedItem.setDocumentId(documentSnapshot.getId());
                                currentRequestItem = refreshedItem;

                                // Update UI with fresh data
                                if (getView() != null) {
                                    updateUIWithRequestItem(getView(), refreshedItem);
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

    private void updateUIWithRequestItem(View view, RequestFood item) {
        ImageView itemImage = view.findViewById(R.id.detail_item_image);
        TextView itemName = view.findViewById(R.id.detail_item_name);
        TextView itemFoodCategory = view.findViewById(R.id.detail_item_category);
        TextView itemUrgencyLevel = view.findViewById(R.id.detail_item_expired_date);
        TextView itemQuantity = view.findViewById(R.id.detail_item_quantity);
        TextView itemPickupTime = view.findViewById(R.id.detail_item_pickup_time);
        TextView itemLocation = view.findViewById(R.id.detail_item_location);
        TextView itemOwner = view.findViewById(R.id.detail_item_owner);
        TextView itemStatus = view.findViewById(R.id.detail_item_status);
        TextView itemCreatedAt = view.findViewById(R.id.detail_item_created_at);
        ImageView ownerProfileImage = view.findViewById(R.id.owner_profile_image);

        // Set up owner profile image
        ownerProfileImage.setClipToOutline(true);
        ownerProfileImage.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
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
        itemFoodCategory.setText("Item Category : " + (item.getFoodCategory() != null ? item.getFoodCategory() : "N/A"));
        itemUrgencyLevel.setText("Urgency Level : " + (item.getUrgencyLevel() != null ? item.getUrgencyLevel() : "N/A"));
        itemQuantity.setText("Quantity : " + (item.getQuantity() != null ? item.getQuantity() : "N/A"));
        itemPickupTime.setText("Available Time : " + (item.getPickupTime() != null ? item.getPickupTime() : "N/A"));
        itemLocation.setText("Location : " + (item.getLocation() != null ? item.getLocation() : "N/A"));

        // Show creation date
        itemCreatedAt.setText("Posted on " + item.getFormattedCreationDate());

        // Show status if completed
        if ("completed".equals(item.getStatus())) {
            itemStatus.setVisibility(View.VISIBLE);
            itemStatus.setText("Status : Completed");
            itemStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            itemStatus.setVisibility(View.GONE);
        }

        // Fetch and display owner's username
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(item.getEmail()) // Assuming owner's email is stored in `item.getOwnerEmail()`
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
        loadOwnerProfileImage(ownerUsername, ownerProfileImage);

        // Load owner's profile image
        Glide.with(this)
                .load(item.getOwnerProfileImageUrl())
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .centerCrop()
                .into(ownerProfileImage);

        // Update buttons visibility based on ownership and status
        updateButtonsVisibility(view, item);
    }

    private void updateButtonsVisibility(View view, RequestFood item) {
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
                    requestButton.setText("Donate!");
                    requestButton.setOnClickListener(v -> acceptRequestItem(item));
                }
            } else {
                Log.e("UpdateVisibility", "Current user's email is null.");
            }
        } else {
            Log.e("UpdateVisibility", "No authenticated user.");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the broadcast receiver
        profileUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String newProfileImageUrl = intent.getStringExtra("newProfileImageUrl");
                String ownerEmail = intent.getStringExtra("ownerEmail");

                // Get the current donation item
                DonationItem currentItem = getArguments() != null ?
                        (DonationItem) getArguments().getSerializable(ARG_FOOD_ITEM) : null;

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

    private void showDeleteConfirmation(RequestFood item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Request")
                .setMessage("Are you sure you want to delete this request?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRequestItem(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRequestItem(RequestFood item) {
        if (item.getDocumentId() == null) {
            Toast.makeText(getContext(), "Error: Cannot delete item without document ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        System.out.println("Deleting item with document ID: " + item.getDocumentId());

        RequestFoodRepo repository = new RequestFoodRepo();
        repository.deleteRequestItem(item.getDocumentId(), new RequestFoodRepo.OnDeleteCompleteListener() {
            @Override
            public void onDeleteSuccess() {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Request deleted successfully", Toast.LENGTH_SHORT).show();
                    // Navigate back
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            }

            @Override
            public void onDeleteFailure(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to delete request: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showCompleteConfirmation(RequestFood item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Complete Request")
                .setMessage("Mark this request as completed?")
                .setPositiveButton("Complete", (dialog, which) -> completeRequest(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void completeRequest(RequestFood item) {
        if (item.getDocumentId() == null) {
            Toast.makeText(getContext(), "Error: Cannot update item without document ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        RequestFoodRepo repository = new RequestFoodRepo();
        repository.updateRequestStatus(item.getDocumentId(), "completed",
                new RequestFoodRepo.OnStatusUpdateListener() {
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

                            Toast.makeText(getContext(),"Request marked as complete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onUpdateFailure(Exception e) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "Failed to update request: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void acceptRequestItem(RequestFood item) {
        if (item.getDocumentId() == null) {
            Toast.makeText(getContext(), "Error: Cannot update item without document ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        RequestFoodRepo repository = new RequestFoodRepo();
        repository.updateRequestStatus(item.getDocumentId(), "completed",
                new RequestFoodRepo.OnStatusUpdateListener() {
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
                                    "Thank you for donating!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onUpdateFailure(Exception e) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "Failed to donate request: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadOwnerProfileImage(String ownerUsername, ImageView ownerProfileImage) {
        // Get the DonationItem from arguments
        if (getArguments() != null) {
            RequestFood donationItem = (RequestFood) getArguments().getSerializable(ARG_FOOD_ITEM);
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

}