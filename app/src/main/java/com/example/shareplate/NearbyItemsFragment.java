package com.example.shareplate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class NearbyItemsFragment extends Fragment {
    private static final String TAG = "NearbyItemsFragment";
    private RecyclerView recyclerView;
    private NearbyItemsAdapter adapter;
    private DonationItemRepository donationItemRepository;
    private String userLocation;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby_items, container, false);

        // Add back button
        ImageView backButton = view.findViewById(R.id.backBtn);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Initialize views
        recyclerView = view.findViewById(R.id.nearby_items_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Show loading initially
        showLoading(true);

        // Get user's location from profile
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            getUserLocation(currentUser.getEmail());
        }

        donationItemRepository = new DonationItemRepository();

        return view;
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void getUserLocation(String userEmail) {
        Log.d(TAG, "getUserLocation: Fetching location for " + userEmail);
        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(documents -> {
                    if (!documents.isEmpty()) {
                        userLocation = documents.getDocuments().get(0).getString("location");
                        Log.d(TAG, "getUserLocation: Found location: " + userLocation);
                        if (userLocation != null && !userLocation.isEmpty()) {
                            loadNearbyItems();
                        } else {
                            showLoading(false);
                            showError("Please enable location services");
                        }
                    } else {
                        Log.d(TAG, "getUserLocation: No location found for user");
                        showLoading(false);
                        showError("Please enable location services");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getUserLocation: Error getting location", e);
                    showLoading(false);
                    showError("Error getting user location: " + e.getMessage());
                });
    }

    private void loadNearbyItems() {
        if (userLocation == null) {
            Log.d(TAG, "loadNearbyItems: No user location available");
            showLoading(false);
            showError("Location not available");
            return;
        }

        Log.d(TAG, "loadNearbyItems: Loading items for location: " + userLocation);
        donationItemRepository.getAllDonationItems(new DonationItemRepository.OnDonationItemsLoadedListener() {
            @Override
            public void onDonationItemsLoaded(List<DonationItem> items) {
                Log.d(TAG, "loadNearbyItems: Loaded " + items.size() + " total items");
                DistanceCalculator calc = new DistanceCalculator("AIzaSyD3paVgDTxJxSRCxUy0cj09SEee_fEB9Zc");

                List<DonationItem> nearbyItems = new ArrayList<>();
                int totalItems = items.size();
                final int[] completedTasks = {0}; // Track completed distance calculations

                for (DonationItem item : items) {
                    if (item.getLocation() != null) {
                        calc.calculateDistance(userLocation, item.getLocation(), new DistanceCalculator.DistanceCalculationCallback() {
                            @Override
                            public void onSuccess(double distance) {
                                Log.d(TAG, "Distance: " + distance + " for item: " + item.getName());
                                if (distance < 4.0) {
                                    nearbyItems.add(item);
                                }

                                // Check if all calculations are done
                                completedTasks[0]++;
                                if (completedTasks[0] == totalItems) {
                                    updateUI(nearbyItems);
                                }
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.e(TAG, "Error calculating distance: " + error);

                                // Still increment the counter to track progress
                                completedTasks[0]++;
                                if (completedTasks[0] == totalItems) {
                                    updateUI(nearbyItems);
                                }
                            }
                        });
                    } else {
                        // No location for this item; increment counter directly
                        completedTasks[0]++;
                        if (completedTasks[0] == totalItems) {
                            updateUI(nearbyItems);
                        }
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "loadNearbyItems: Error loading items", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showError("Error loading nearby items: " + e.getMessage());
                    });
                }
            }
        });
    }

    private void updateUI(List<DonationItem> nearbyItems) {
        getActivity().runOnUiThread(() -> {
            if (!nearbyItems.isEmpty()) {
                adapter = new NearbyItemsAdapter(nearbyItems, item -> {
                    Fragment detailFragment;
                    if ("Food".equals(item.getCategory())) {
                        detailFragment = FoodItemDetailFragment.newInstance(item);
                    } else {
                        detailFragment = NonFoodItemDetail.newInstance(item);
                    }

                    // Navigate to the detail fragment
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, detailFragment)
                            .addToBackStack(null)
                            .commit();
                });
                recyclerView.setAdapter(adapter);
            } else {
                showError("No items found near " + userLocation);
            }
            showLoading(false);
        });
    }
}