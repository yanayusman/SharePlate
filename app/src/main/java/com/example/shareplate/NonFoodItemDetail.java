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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class NonFoodItemDetail extends Fragment {
    private static final String ARG_ITEM = "non_food_item";
    private String ownerUsername;
    private BroadcastReceiver profileUpdateReceiver;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DonationItem currentDonationItem;
    private RecyclerView detailRecyclerView;

    public static NonFoodItemDetail newInstance(DonationItem item) {
        NonFoodItemDetail fragment = new NonFoodItemDetail();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_non_food_item_detail, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the NonFoodItem from arguments once
        if (getArguments() != null) {
            currentDonationItem = (DonationItem) getArguments().getSerializable(ARG_ITEM);
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

        swipeRefreshLayout.setOnRefreshListener(this::refreshItemDetails);

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
        View contentView = LayoutInflater.from(requireContext()).inflate(R.layout.non_food_item_detail_content, null);

        // Setup views and load data
        setupViews(contentView);

        // Create a simple adapter with single item
        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.non_food_item_detail_content, parent, false);
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
        // Update the UI with the donation item details
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

    private void refreshItemDetails() {
        if (currentDonationItem == null || currentDonationItem.getEmail() == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String ownerEmail = currentDonationItem.getEmail();

        // Query Firestore for the document with the matching ownerEmail
        FirebaseFirestore.getInstance()
                .collection("allDonationItems")
                .whereEqualTo("email", ownerEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        try {
                            // Assuming only one item matches the ownerUsername
                            DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);

                            // Create a new DonationItem from the fresh data
                            DonationItem refreshedItem = documentSnapshot.toObject(DonationItem.class);
                            if (refreshedItem != null) {
                                refreshedItem.setDocumentId(documentSnapshot.getId());
                                currentDonationItem = refreshedItem;

                                // Update UI with fresh data
                                if (getView() != null) {
                                    updateUIWithDonationItem(getView(), refreshedItem);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("ItemDetail", "Error refreshing data", e);
                            Toast.makeText(getContext(),
                                    "Error refreshing data: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("ItemDetail", "No matching items found for email: " + ownerEmail);
                        Toast.makeText(getContext(),
                                "No matching items found for this user.",
                                Toast.LENGTH_SHORT).show();
                    }
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("ItemDetail", "Failed to refresh", e);
                    Toast.makeText(getContext(),
                            "Failed to refresh: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }


    private void updateUIWithDonationItem(View view, DonationItem item) {
        ImageView itemImage = view.findViewById(R.id.detail_item_image);
        TextView itemName = view.findViewById(R.id.detail_item_name);
        TextView itemCategory = view.findViewById(R.id.detail_item_category);
        TextView itemDescription = view.findViewById(R.id.detail_item_expired_date);
        TextView itemQuantity = view.findViewById(R.id.detail_item_quantity);
        TextView itemPickupTime = view.findViewById(R.id.detail_item_pickup_time);
        TextView itemLocation = view.findViewById(R.id.detail_item_location);
        TextView itemOwner = view.findViewById(R.id.detail_item_owner);
        TextView itemStatus = view.findViewById(R.id.detail_item_status);
        TextView itemCreatedAt = view.findViewById(R.id.detail_item_created_at);
        ImageView ownerProfileImage = view.findViewById(R.id.owner_profile_image);

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
        itemCategory.setText("Item Category : " + (item.getCategory() != null ? item.getCategory() : "N/A"));
        itemDescription.setText("Description : " + (item.getDescription() != null ? item.getDescription() : "N/A"));
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

        // Update buttons visibility based on ownership and status
        updateButtonsVisibility(view, item);

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
                DonationItemRepository repository = new DonationItemRepository();
                repository.updateDonationStatus(item.getDocumentId(), "completed",
                        new DonationItemRepository.OnStatusUpdateListener() {
                            @Override
                            public void onUpdateSuccess() {
                                if (getContext() != null) {
                                    // Update the UI to show completed status
                                    TextView itemStatus = getView().findViewById(R.id.detail_item_status);
                                    itemStatus.setVisibility(View.VISIBLE);
                                    itemStatus.setText("Status : Completed");
                                    itemStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                                    // Hide the complete button
                                    Button completeButton = getView().findViewById(R.id.completeButton);
                                    completeButton.setVisibility(View.GONE);

                                    // Hide the request button
                                    Button requestButton = getView().findViewById(R.id.requestButton);
                                    requestButton.setVisibility(View.GONE);

                                    Toast.makeText(getContext(),
                                            "Thank you for donating!", Toast.LENGTH_SHORT).show();
                                            
                                    // Broadcast the update
                                    Intent refreshIntent = new Intent("profile.stats.updated");
                                    LocalBroadcastManager.getInstance(requireContext())
                                            .sendBroadcast(refreshIntent);
                                }
                            }

                            @Override
                            public void onUpdateFailure(Exception e) {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(),
                                            "Failed to request: " + e.getMessage(),
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
                        (DonationItem) getArguments().getSerializable(ARG_ITEM) : null;

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

        NonFoodItemRepository repository = new NonFoodItemRepository();
        repository.deleteNonFoodItem(item.getDocumentId(), new NonFoodItemRepository.OnDeleteCompleteListener() {
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

        NonFoodItemRepository repository = new NonFoodItemRepository();
        repository.updateDonationStatus(item.getDocumentId(), "completed",
                new NonFoodItemRepository.OnStatusUpdateListener() {
                    @Override
                    public void onUpdateSuccess() {
                        if (getContext() != null) {
                            // Update the UI to show completed status
                            TextView itemStatus = getView().findViewById(R.id.detail_item_status);
                            itemStatus.setVisibility(View.VISIBLE);
                            itemStatus.setText("Status : Completed");
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
            DonationItem donationItem = (DonationItem) getArguments().getSerializable(ARG_ITEM);
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
        EditNonFoodFragment editFragment = EditNonFoodFragment.newInstance(currentDonationItem);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    private void openLocationInMaps(String location) {
        // ... same implementation as above ...
    }
}