package com.example.shareplate;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.text.SimpleDateFormat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeFragment extends Fragment {

    private Toolbar toolbar;
    private ImageView searchIcon, menuIcon, backArrow, sortIcon;
    private GridLayout donationGrid;
    private EditText searchEditText;
    private LinearLayout searchLayout;
    private View normalToolbarContent;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DonationItemRepository donationItemRepository;
    private RequestFoodRepo requestFoodRepo;
    private List<Object> allItems = new ArrayList<>();
    private SortOption currentSortOption = SortOption.DEFAULT;
    private SortDirection currentSortDirection = SortDirection.ASCENDING;
    private View cachedSearchLayout;
    private View cachedNormalLayout;
    private boolean isSearchMode = false;
    private static final int SEARCH_DELAY = 100; // ms
    private Runnable searchRunnable;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private InputMethodManager imm;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);

        checkUserExistence();
        // Initialize views and repository
        initializeViews(rootView);
        donationItemRepository = new DonationItemRepository();
        requestFoodRepo = new RequestFoodRepo();

        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadItems);

        // Load donation items from Firestore
        loadItems();

        // Set up search functionality
        searchIcon.setOnClickListener(v -> {
            if (isSearchMode) return;
            isSearchMode = true;
            
            // Hardware acceleration for animations
            searchLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            
            // Update visibilities without triggering layout
            searchLayout.setVisibility(View.VISIBLE);
            normalToolbarContent.setVisibility(View.GONE);
            toolbar.setVisibility(View.GONE);
            
            // Focus and keyboard handling
            searchEditText.requestFocus();
            if (imm != null) {
                imm.showSoftInput(searchEditText, 0);
            }
            
            // Reset layer type after transition
            searchLayout.post(() -> searchLayout.setLayerType(View.LAYER_TYPE_NONE, null));
        });

        backArrow.setOnClickListener(v -> closeSearch());

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterItems(s.toString());
            }
        });

        Button sortButton = rootView.findViewById(R.id.sortButton);
        sortButton.setOnClickListener(v -> showSortOptions());

        /* Comment out Urgent Donation Card components
        CardView urgentDonationCard = rootView.findViewById(R.id.urgent_donation_card);
        TextView urgentText = rootView.findViewById(R.id.urgent_text);
        TextView itemTitle = rootView.findViewById(R.id.item_title);
        TextView pickupText = rootView.findViewById(R.id.pickup_text);
        ImageView itemImage = rootView.findViewById(R.id.item_image);
        TextView foodItemText = rootView.findViewById(R.id.food_item_text);
        TextView expiresText = rootView.findViewById(R.id.expires_text);
        TextView quantityText = rootView.findViewById(R.id.quantity_text);
        TextView pickupTimeText = rootView.findViewById(R.id.pickup_time_text);
        TextView perishableText = rootView.findViewById(R.id.perishable_text);
        */

        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        // Pre-initialize IMM at fragment creation
        imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private void initializeViews(View view) {
        // Initialize all views first
        toolbar = view.findViewById(R.id.toolbar);
        searchIcon = view.findViewById(R.id.search_icon);
//        menuIcon = view.findViewById(R.id.menu_icon);
        donationGrid = view.findViewById(R.id.donation_grid);
        searchEditText = view.findViewById(R.id.search_edit_text);
        searchLayout = view.findViewById(R.id.search_layout);
        backArrow = view.findViewById(R.id.back_arrow);
        normalToolbarContent = view.findViewById(R.id.normal_toolbar_content);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        sortIcon = view.findViewById(R.id.sort_icon);

        // Set up sort functionality
        sortIcon.setOnClickListener(v -> showSortOptions());

        // Add notification icon click listener
        ImageView notificationIcon = view.findViewById(R.id.notification_icon);
        notificationIcon.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new NotificationAll())
                    .addToBackStack(null)
                    .commit();
        });

        ImageView searchBackButton = view.findViewById(R.id.search_back_button);
        searchBackButton.setOnClickListener(v -> closeSearch());

        // Pre-cache layouts and set initial states
        searchLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        normalToolbarContent.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        toolbar.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // Pre-measure and cache layouts
        preWarmSearchLayout();
        
        // Optimize search icon for faster touch response
        searchIcon.setClickable(true);
        searchIcon.setFocusable(true);
        searchIcon.setOnTouchListener((v, event) -> {
            if (isSearchMode) return false;
            showSearch();
            return true;
        });

        // Optimize back button
        searchBackButton.setClickable(true);
        searchBackButton.setFocusable(true);
        searchBackButton.setOnTouchListener((v, event) -> {
            if (!isSearchMode) return false;
            closeSearch();
            return true;
        });

        // Optimize search text changes with debouncing
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> filterItems(s.toString());
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }
        });
    }

    private void checkUserExistence(){
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String email = currentUser.getEmail();

                if (email != null) {
                    db.collection("users")
                            .document(email)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (!documentSnapshot.exists()) {
                                    // Create a new user document if it doesn't exist
                                    Map<String, Object> userData = new HashMap<>();
                                    userData.put("email", email);
                                    userData.put("username", "");
                                    userData.put("location", "");
                                    userData.put("phoneNumber", "");
                                    userData.put("profileImage", "");

                                    db.collection("users")
                                            .document(email)
                                            .set(userData)
                                            .addOnSuccessListener(aVoid -> Log.d("Firestore", "User document created"))
                                            .addOnFailureListener(e -> Log.e("FirestoreError", "Failed to create user document: " + e.getMessage()));
                                }
                            })
                            .addOnFailureListener(e -> Log.e("FirestoreError", "Error checking user document: " + e.getMessage()));
                }
    }

    private void loadItems() {
        swipeRefreshLayout.setRefreshing(true);
        allItems.clear();

        // Load DonationItems
        donationItemRepository.getAllDonationItems(new DonationItemRepository.OnDonationItemsLoadedListener() {
            @Override
            public void onDonationItemsLoaded(List<DonationItem> donationItems) {
                allItems.addAll(donationItems);
                // After loading donation items, load RequestFood items
                loadRequestItems();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getActivity(), "Error loading donation items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void loadRequestItems() {
        requestFoodRepo.getAllRequestItem(new RequestFoodRepo.OnRequestItemsLoadedListener() {
            @Override
            public void onRequestItemsLoaded(List<RequestFood> requestItems) {
                allItems.addAll(requestItems);
                // After loading all items, display them
                displayItems();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getActivity(), "Error loading request items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void displayItems() {
        // Check if fragment is attached
        if (!isAdded() || getActivity() == null) {
            return;
        }

        try {
            // Clear grid and sort items
            donationGrid.removeAllViews();
            Collections.sort(allItems, (a, b) -> {
                long aTime = (a instanceof DonationItem) ? ((DonationItem) a).getCreatedAt() : ((RequestFood) a).getCreatedAt();
                long bTime = (b instanceof DonationItem) ? ((DonationItem) b).getCreatedAt() : ((RequestFood) b).getCreatedAt();
                return Long.compare(bTime, aTime); // Descending order
            });

            // Add views for all items
            for (Object item : allItems) {
                if (item instanceof DonationItem) {
                    addDonationItemView((DonationItem) item);
                } else if (item instanceof RequestFood) {
                    addRequestItemView((RequestFood) item);
                }
            }

            swipeRefreshLayout.setRefreshing(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addDonationItemView(DonationItem item) {
        // Check if fragment is attached
        if (!isAdded() || getActivity() == null || getContext() == null) {
            return;
        }

        try {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View itemView = inflater.inflate(R.layout.donation_item_view, donationGrid, false);

            ImageView itemImage = itemView.findViewById(R.id.item_image);
            TextView itemName = itemView.findViewById(R.id.item_name);
            TextView itemCategory = itemView.findViewById(R.id.item_category);
            TextView itemInfo = itemView.findViewById(R.id.item_expiredDate);
            TextView itemQuantity = itemView.findViewById(R.id.item_quantity);
            TextView itemPickupTime = itemView.findViewById(R.id.item_pickupTime);
            TextView itemLocation = itemView.findViewById(R.id.item_distance);
            TextView statusIndicator = itemView.findViewById(R.id.status_indicator);

            // Show status indicator if item is completed
            if (item.getStatus() != null && item.getStatus().equals("completed")) {
                statusIndicator.setVisibility(View.VISIBLE);
                // Optional: Add some visual dimming to the entire card
                itemView.setAlpha(0.8f);
            } else {
                statusIndicator.setVisibility(View.GONE);
                itemView.setAlpha(1.0f);
            }

            // Load image using Glide
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

            itemName.setText(item.getName());

            if ("Food".equals(item.getDonateType())) {
                itemCategory.setText("Food Category : " + (item.getFoodCategory() != null ? item.getFoodCategory() : "N/A"));
                itemInfo.setText("Expires : " + (item.getExpiredDate() != null ? item.getExpiredDate() : "N/A"));
                itemQuantity.setText("Quantity : " + (item.getQuantity() != null ? item.getQuantity() : "N/A"));
                itemPickupTime.setText("Pickup Time : " + (item.getPickupTime() != null ? item.getPickupTime() : "N/A"));
                itemLocation.setText(item.getLocation());

                itemView.setOnClickListener(v -> {
                    FoodItemDetailFragment detailFragment = FoodItemDetailFragment.newInstance(item);
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, detailFragment)
                            .addToBackStack(null)
                            .commit();
                });
            } else {
                itemCategory.setText("Item Category : " + (item.getCategory() != null ? item.getCategory() : "N/A"));
                itemInfo.setText("Description : " + (item.getDescription() != null ? item.getDescription() : "N/A"));
                itemQuantity.setText("Quantity : " + (item.getQuantity() != null ? item.getQuantity() : "N/A"));
                itemPickupTime.setText("Pickup Time : " + (item.getPickupTime() != null ? item.getPickupTime() : "N/A"));
                itemLocation.setText(item.getLocation());

                itemView.setOnClickListener(v -> {
                    NonFoodItemDetail detailFragment = NonFoodItemDetail.newInstance(item);
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, detailFragment)
                            .addToBackStack(null)
                            .commit();
                });
            }

            itemQuantity.setText("Quantity : " + (item.getQuantity() != null ? item.getQuantity() : "N/A"));
            itemPickupTime.setText("Pickup Time : " + (item.getPickupTime() != null ? item.getPickupTime() : "N/A"));
            itemLocation.setText(item.getLocation());

            donationGrid.addView(itemView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addRequestItemView(RequestFood item) {
        View itemView = getLayoutInflater().inflate(R.layout.donation_item_view, donationGrid, false);

        ImageView itemImage = itemView.findViewById(R.id.item_image);
        TextView itemName = itemView.findViewById(R.id.item_name);
        TextView itemCategory = itemView.findViewById(R.id.item_category);
        TextView itemInfo = itemView.findViewById(R.id.item_expiredDate);
        TextView itemQuantity = itemView.findViewById(R.id.item_quantity);
        TextView itemPickupTime = itemView.findViewById(R.id.item_pickupTime);
        TextView itemLocation = itemView.findViewById(R.id.item_distance);
        TextView statusIndicator = itemView.findViewById(R.id.status_indicator);

        // Show status indicator if item is completed
        if (item.getStatus() != null && item.getStatus().equals("completed")) {
            statusIndicator.setVisibility(View.VISIBLE);
            // Optional: Add some visual dimming to the entire card
            itemView.setAlpha(0.8f);
        } else {
            statusIndicator.setVisibility(View.GONE);
            itemView.setAlpha(1.0f);
        }

        // Load image using Glide
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

        itemName.setText(item.getName());

        if ("Food".equals(item.getDonateType())) {
            itemCategory.setText("Food Category : " + (item.getFoodCategory() != null ? item.getFoodCategory() : "N/A"));
            itemInfo.setText("Urgency Level : " + (item.getUrgencyLevel() != null ? item.getUrgencyLevel() : "N/A"));
            itemQuantity.setText("Quantity : " + (item.getQuantity() != null ? item.getQuantity() : "N/A"));
            itemLocation.setText(item.getLocation());

            itemPickupTime.setVisibility(View.GONE);
            itemView.setOnClickListener(v -> {
                RequestItemDetailFragment detailFragment = RequestItemDetailFragment.newInstance(item);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            });
        } else {
            itemCategory.setText("Item Category : " + (item.getFoodCategory() != null ? item.getFoodCategory() : "N/A"));
            itemInfo.setText("Urgency Level : " + (item.getUrgencyLevel() != null ? item.getUrgencyLevel() : "N/A"));
            itemQuantity.setText("Quantity : " + (item.getQuantity() != null ? item.getQuantity() : "N/A"));
            itemLocation.setText(item.getLocation());

            itemPickupTime.setVisibility(View.GONE);
            itemView.setOnClickListener(v -> {
                RequestItemDetailFragment detailFragment = RequestItemDetailFragment.newInstance(item);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }
        donationGrid.addView(itemView);
    }

    private void filterItems(String query) {
        if (query == null || query.isEmpty()) {
            displayItems(); // Show all items if query is empty
            return;
        }

        // Optimize filtering
        String lowercaseQuery = query.toLowerCase();
        List<Object> filteredItems = new ArrayList<>();
        
        // Use direct iteration instead of stream for better performance
        for (Object item : allItems) {
            String name = null;
            if (item instanceof DonationItem) {
                name = ((DonationItem) item).getName();
            } else if (item instanceof RequestFood) {
                name = ((RequestFood) item).getName();
            }
            
            if (name != null && name.toLowerCase().contains(lowercaseQuery)) {
                filteredItems.add(item);
            }
        }

        // Update UI on next frame
        rootView.post(() -> {
            donationGrid.removeAllViews();
            for (Object item : filteredItems) {
                if (item instanceof DonationItem) {
                    addDonationItemView((DonationItem) item);
                } else if (item instanceof RequestFood) {
                    addRequestItemView((RequestFood) item);
                }
            }
        });
    }

    private void showSortOptions() {
        String[] options = Arrays.stream(SortOption.values())
                .map(SortOption::getDisplayName)
                .toArray(String[]::new);

        new AlertDialog.Builder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(options, currentSortOption.ordinal(), (dialog, which) -> {
                    currentSortOption = SortOption.values()[which];
                    if (currentSortOption != SortOption.DEFAULT) {
                        showSortDirectionDialog();
                    } else {
                        sortItems();
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private void showSortDirectionDialog() {
        String[] directions = Arrays.stream(SortDirection.values())
                .map(SortDirection::getDisplayName)
                .toArray(String[]::new);

        new AlertDialog.Builder(requireContext())
                .setTitle("Sort Direction")
                .setSingleChoiceItems(directions, currentSortDirection.ordinal(), (dialog, which) -> {
                    currentSortDirection = SortDirection.values()[which];
                    sortItems();
                    dialog.dismiss();
                })
                .show();
    }

    private void sortItems() {
        if (allItems == null || allItems.isEmpty()) return;

        List<Object> sortedItems = new ArrayList<>(allItems);

        switch (currentSortOption) {
            case DATE_CREATED:
                sortedItems.sort((a, b) -> {
                    long timeA = (a instanceof DonationItem) ? ((DonationItem) a).getCreatedAt() : ((RequestFood) a).getCreatedAt();
                    long timeB = (b instanceof DonationItem) ? ((DonationItem) b).getCreatedAt() : ((RequestFood) b).getCreatedAt();
                    return currentSortDirection == SortDirection.ASCENDING ?
                            Long.compare(timeA, timeB) : Long.compare(timeB, timeA);
                });
                break;

            case EXPIRY_DATE:
                sortedItems.sort((a, b) -> {
                    if (a instanceof DonationItem && b instanceof DonationItem) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                            Date dateA = sdf.parse(((DonationItem) a).getExpiredDate());
                            Date dateB = sdf.parse(((DonationItem) b).getExpiredDate());
                            return currentSortDirection == SortDirection.ASCENDING ?
                                    dateA.compareTo(dateB) : dateB.compareTo(dateA);
                        } catch (Exception e) {
                            return 0;
                        }
                    }
                    return 0; // Skip non-DonationItem objects
                });
                break;

            case CATEGORY:
                sortedItems.sort((a, b) -> {
                    if (a instanceof DonationItem && b instanceof DonationItem) {
                        int result = ((DonationItem) a).getFoodCategory()
                                .compareToIgnoreCase(((DonationItem) b).getFoodCategory());
                        return currentSortDirection == SortDirection.ASCENDING ? result : -result;
                    }
                    return 0; // Skip non-DonationItem objects
                });
                break;

            case QUANTITY:
                sortedItems.sort((a, b) -> {
                    if (a instanceof DonationItem && b instanceof DonationItem) {
                        try {
                            int qtyA = Integer.parseInt(((DonationItem) a).getQuantity().replaceAll("[^0-9]", ""));
                            int qtyB = Integer.parseInt(((DonationItem) b).getQuantity().replaceAll("[^0-9]", ""));
                            return currentSortDirection == SortDirection.ASCENDING ?
                                    Integer.compare(qtyA, qtyB) : Integer.compare(qtyB, qtyA);
                        } catch (Exception e) {
                            return 0;
                        }
                    }
                    return 0; // Skip non-DonationItem objects
                });
                break;

            case LOCATION:
                sortedItems.sort((a, b) -> {
                    if (a instanceof DonationItem && b instanceof DonationItem) {
                        int result = ((DonationItem) a).getLocation()
                                .compareToIgnoreCase(((DonationItem) b).getLocation());
                        return currentSortDirection == SortDirection.ASCENDING ? result : -result;
                    }
                    return 0; // Skip non-DonationItem objects
                });
                break;

            case PICKUP_TIME:
                sortedItems.sort((a, b) -> {
                    if (a instanceof DonationItem && b instanceof DonationItem) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
                            Date timeA = sdf.parse(((DonationItem) a).getPickupTime());
                            Date timeB = sdf.parse(((DonationItem) b).getPickupTime());
                            return currentSortDirection == SortDirection.ASCENDING ?
                                    timeA.compareTo(timeB) : timeB.compareTo(timeA);
                        } catch (Exception e) {
                            return 0;
                        }
                    }
                    return 0; // Skip non-DonationItem objects
                });
                break;

            case DEFAULT:
            default:
                // Do nothing, keep original order
                break;
        }

        // Clear and reload the grid with sorted items
        donationGrid.removeAllViews();
        for (Object item : sortedItems) {
            if (item instanceof DonationItem) {
                addDonationItemView((DonationItem) item);
            } else if (item instanceof RequestFood) {
                addRequestItemView((RequestFood) item);
            }
        }
    }

    private void closeSearch() {
        if (!isSearchMode) return;
        isSearchMode = false;
        
        // Hide keyboard first since it's the most time-consuming operation
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        }
        
        // Batch view updates in a single operation
        searchLayout.setVisibility(View.GONE);
        normalToolbarContent.setVisibility(View.VISIBLE);
        toolbar.setVisibility(View.VISIBLE);
        
        // Clear search text without animation
        searchEditText.setText(null);
    }

    private void showSearch() {
        if (isSearchMode) return;
        isSearchMode = true;

        // Pre-fetch next frame
        rootView.post(() -> {
            // Hardware acceleration
            searchLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            
            // Update visibilities in a single frame
            searchLayout.setAlpha(1f);
            searchLayout.setVisibility(View.VISIBLE);
            normalToolbarContent.setVisibility(View.GONE);
            toolbar.setVisibility(View.GONE);
            
            // Show keyboard
            searchEditText.requestFocus();
            if (imm != null) {
                imm.showSoftInput(searchEditText, 0);
            }
        });
    }

    // Add method to pre-warm the search layout
    private void preWarmSearchLayout() {
        if (searchLayout != null) {
            searchLayout.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            searchLayout.layout(0, 0, searchLayout.getMeasuredWidth(), searchLayout.getMeasuredHeight());
        }
    }
}