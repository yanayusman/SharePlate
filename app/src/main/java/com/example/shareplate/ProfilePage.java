package com.example.shareplate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;
import android.net.Uri;
import android.provider.MediaStore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.RequestBuilder;
import java.util.Map;
import java.util.HashMap;

public class ProfilePage extends Fragment {
    private static final String TAG = "ProfilePage";
    private final String COLLECTION_USER = "users";
    private final String COLLECTION_DONATE = "allDonationItems";
    private final String COLLECTION_REQUEST = "foodRequest";

    private final String SUBCOLLECTION_USER = "joinedEvents";
    private ImageView profileImage, notificationIV;
    private TextView  donatedCountTV, requestedCountTV, campaignsCountTV, volunteerCountTV, profileUsername;
    private MaterialButton editProfileButton, rateAppButton, termsConditionButton, signOutButton, resetPass;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String userEmail;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FirebaseFirestore db;
    private BroadcastReceiver statsUpdateReceiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Add this: Initialize the stats update receiver
        statsUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals("profile.stats.updated")) {
                    // Refresh the profile stats
                    fetchCurrentUserDetails();
                }
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_page, container, false);

        Log.d(TAG, "onCreateView: Initializing views");

        // Initialize views
        profileImage = view.findViewById(R.id.profile_image);
        profileUsername = view.findViewById(R.id.profile_username);
        signOutButton = view.findViewById(R.id.signOutButton);
        if (signOutButton == null) {
            Log.e(TAG, "onCreateView: signOutButton not found in layout");
        }

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::refreshProfile);
        }

        // Set up click listeners and other UI components
        setupClickListeners(view);

        // Load user details
        fetchCurrentUserDetails();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register receivers
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity())
                    .registerReceiver(profileUpdateReceiver, new IntentFilter("profile.image.updated"));
            LocalBroadcastManager.getInstance(getActivity())
                    .registerReceiver(usernameUpdateReceiver, new IntentFilter("profile.username.updated"));
            LocalBroadcastManager.getInstance(getActivity())
                    .registerReceiver(statsUpdateReceiver, new IntentFilter("profile.stats.updated"));
        }
        
        // Refresh profile data
        fetchCurrentUserDetails();
    }

    private void fetchCurrentUserDetails() {
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found");
            if (profileUsername != null) {
                profileUsername.setText("User not logged in");
            }
            return;
        }

        // Get the user's email or Facebook ID
        String documentId = null;
        if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
            documentId = currentUser.getEmail();
        } else {
            // Try to get Facebook ID from provider data
            for (UserInfo profile : currentUser.getProviderData()) {
                if (profile.getProviderId().equals("facebook.com")) {
                    documentId = profile.getUid() + "@facebook.com";
                    break;
                }
            }
        }

        if (documentId == null) {
            Log.w(TAG, "Could not determine user document ID");
            if (profileUsername != null) {
                profileUsername.setText("User not logged in");
            }
            return;
        }

        final String userDocId = documentId;
        db.collection(COLLECTION_USER)
                .document(userDocId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String username = document.getString("username");
                        if (username == null || username.isEmpty()) {
                            // If no username in Firestore, check if we have a display name from auth provider
                            username = currentUser.getDisplayName();
                            if (username == null || username.isEmpty()) {
                                // If still no username, use email prefix or Facebook ID
                                username = userDocId.substring(0, userDocId.indexOf('@'));
                                // Update Firestore with this default username
                                final String finalUsername = username;
                                document.getReference().update("username", finalUsername)
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error updating default username", e);
                                        });
                            }
                        }
                        
                        // Update the UI with the username
                        final String displayUsername = username;
                        if (profileUsername != null) {
                            profileUsername.setText(displayUsername);
                        }

                        // Load profile image
                        loadProfileImage();

                        // Fetch counts
                        fetchDonatedCount(displayUsername);
                        fetchRequestedCount(displayUsername);
                        fetchCampaignsCount(displayUsername);
                        fetchVolunteersCount(displayUsername);
                    } else {
                        // Create a new user document if it doesn't exist
                        final String defaultUsername;
                        String tempUsername = currentUser.getDisplayName();
                        if (tempUsername == null || tempUsername.isEmpty()) {
                            tempUsername = userDocId.substring(0, userDocId.indexOf('@'));
                        }
                        defaultUsername = tempUsername;
                        
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", userDocId);
                        userData.put("username", defaultUsername);
                        userData.put("location", "");
                        userData.put("phoneNumber", "");

                        db.collection(COLLECTION_USER)
                                .document(userDocId)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User document created successfully");
                                    if (profileUsername != null) {
                                        profileUsername.setText(defaultUsername);
                                    }
                                    // Fetch counts after creating the document
                                    fetchDonatedCount(defaultUsername);
                                    fetchRequestedCount(defaultUsername);
                                    fetchCampaignsCount(defaultUsername);
                                    fetchVolunteersCount(defaultUsername);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error creating user document", e);
                                    if (profileUsername != null) {
                                        profileUsername.setText(userDocId.substring(0, userDocId.indexOf('@')));
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user details", e);
                    if (profileUsername != null) {
                        profileUsername.setText(userDocId.substring(0, userDocId.indexOf('@')));
                    }
                });
    }

    // Add a new method to handle username updates
    private BroadcastReceiver usernameUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String newUsername = intent.getStringExtra("newUsername");
            if (newUsername != null && !newUsername.isEmpty()) {
//                usernameTextView.setText(newUsername);
                profileUsername.setText(newUsername);
            }
        }
    };

    private BroadcastReceiver profileUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String newProfileImageUrl = intent.getStringExtra("newProfileImageUrl");
            if (newProfileImageUrl != null && !newProfileImageUrl.isEmpty() && profileImage != null) {
                Glide.with(ProfilePage.this)
                        .load(newProfileImageUrl)
                        .circleCrop()
                        .into(profileImage);
            }
        }
    };

    private void setResetPassClickListener() {
        resetPass.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ResetPasswordFragment())
                .addToBackStack(null)
                .commit());
    }

    private void fetchDonatedCount(String username) {
        FirebaseUser currUser = mAuth.getCurrentUser();
        if (currUser == null) return;

        // Query the donations collection where donorUsername matches
        db.collection(COLLECTION_DONATE)
                .whereEqualTo("email", getDocumentId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        int donatedCountValue = 0;

                        // Iterate through documents and count donations
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            donatedCountValue++;
                        }

                        // Update the TextView
                        if (donatedCountTV != null) {
                            donatedCountTV.setText(String.valueOf(donatedCountValue));
                        }
                    } else {
                        // No donations found
                        if (donatedCountTV != null) {
                            donatedCountTV.setText("0");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching donated items count", e);
                    if (donatedCountTV != null) {
                        donatedCountTV.setText("0");
                    }
                });
    }

    private void fetchRequestedCount(String username) {
        FirebaseUser currUser = mAuth.getCurrentUser();
        if (currUser == null) return;

        // Query the donations collection where donorUsername matches
        db.collection(COLLECTION_REQUEST)
                .whereEqualTo("email", getDocumentId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        int requestedCountValue = 0;

                        // Iterate through documents and count donations
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            requestedCountValue++;
                        }

                        // Update the TextView
                        if (requestedCountTV != null) {
                            requestedCountTV.setText(String.valueOf(requestedCountValue));
                        }
                    } else {
                        // No donations found
                        if (requestedCountTV != null) {
                            requestedCountTV.setText("0");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching requested items count", e);
                    if (requestedCountTV != null) {
                        requestedCountTV.setText("0");
                    }
                });
    }

    private void fetchCampaignsCount(String username) {
        String documentId = getDocumentId();
        if (documentId == null) {
            Log.e(TAG, "Cannot fetch campaigns count: document ID is null");
            if (campaignsCountTV != null) {
                campaignsCountTV.setText("0");
            }
            return;
        }

        // Query the donations collection where donorUsername matches
        db.collection(COLLECTION_USER)
                .document(documentId)
                .collection(SUBCOLLECTION_USER)
                .whereEqualTo("eventType", "Campaigns")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        int campaignsCountValue = 0;

                        // Iterate through documents and count donations
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            campaignsCountValue++;
                        }

                        // Update the TextView
                        if (campaignsCountTV != null) {
                            campaignsCountTV.setText(String.valueOf(campaignsCountValue));
                        }
                    } else {
                        // No donations found
                        if (campaignsCountTV != null) {
                            campaignsCountTV.setText("0");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching campaigns items count", e);
                    if (campaignsCountTV != null) {
                        campaignsCountTV.setText("0");
                    }
                });
    }

    private void fetchVolunteersCount(String username) {
        String documentId = getDocumentId();
        if (documentId == null) {
            Log.e(TAG, "Cannot fetch volunteers count: document ID is null");
            if (volunteerCountTV != null) {
                volunteerCountTV.setText("0");
            }
            return;
        }

        // Query the donations collection where donorUsername matches
        db.collection(COLLECTION_USER)
                .document(documentId)
                .collection(SUBCOLLECTION_USER)
                .whereEqualTo("eventType", "Volunteering")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        int volunteersCountValue = 0;

                        // Iterate through documents and count donations
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            volunteersCountValue++;
                        }

                        // Update the TextView
                        if (volunteerCountTV != null) {
                            volunteerCountTV.setText(String.valueOf(volunteersCountValue));
                        }
                    } else {
                        // No donations found
                        if (volunteerCountTV != null) {
                            volunteerCountTV.setText("0");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching campaigns items count", e);
                    if (volunteerCountTV != null) {
                        volunteerCountTV.setText("0");
                    }
                });
    }

    private String getDocumentId() {
        if (currentUser == null) return null;

        // Get the user's email or Facebook ID
        if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
            return currentUser.getEmail();
        }

        // Try to get Facebook ID from provider data
        for (UserInfo profile : currentUser.getProviderData()) {
            if (profile.getProviderId().equals("facebook.com")) {
                return profile.getUid() + "@facebook.com";
            }
        }

        return null;
    }

    private void signOut() {
        Log.d(TAG, "signOut: Showing confirmation dialog");

        // Create and show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Log.d(TAG, "signOut: User confirmed sign out");
                    performSignOut();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    Log.d(TAG, "signOut: User cancelled sign out");
                    dialog.dismiss();
                })
                .show();
    }

    private void performSignOut() {
        Log.d(TAG, "performSignOut: Attempting to sign out user");
        try {
            mAuth.signOut();
            Log.d(TAG, "performSignOut: User signed out successfully");
            Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();

            // Navigate to MainActivity
            Intent intent = new Intent(getActivity(), MainActivity.class);
            // Clear the back stack
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Finish the current activity
            if (getActivity() != null) {
                getActivity().finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "performSignOut: Failed to sign out", e);
            Toast.makeText(getContext(), "Sign out failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfileImage(Uri imageUri) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        StorageReference imageRef = storageRef.child("profile_images/" + userId + ".jpg");

        // Show loading state
        if (getView() != null) {
            getView().findViewById(R.id.edit_profile_image).setEnabled(false);
            // Show the refresh animation
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(true);
            }
        }

        // Upload the new image
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL and update profile
                    imageRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                // Update Firebase Auth user profile
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setPhotoUri(downloadUri)
                                        .build();

                                currentUser.updateProfile(profileUpdates)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "User profile photo updated successfully");
                                            // Force refresh the user data
                                            currentUser.reload().addOnCompleteListener(task -> {
                                                // Update UI after reload
                                                updateProfileImage(downloadUri.toString());
                                                // Refresh the entire profile
                                                refreshProfile();
                                            });
                                            // Update all donations with the new profile image URL
                                            updateDonationsProfileImage(downloadUri.toString(), currentUser.getDisplayName());

                                            // Save the profile image URL to SharedPreferences
                                            if (getContext() != null) {
                                                getContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                                        .edit()
                                                        .putString("default_profile_image", downloadUri.toString())
                                                        .apply();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to update user profile photo", e);
                                            if (swipeRefreshLayout != null) {
                                                swipeRefreshLayout.setRefreshing(false);
                                            }
                                        });

                                // Show success message
                                Toast.makeText(getContext(), "Profile picture updated successfully",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to get download URL", e);
                                Toast.makeText(getContext(),
                                        "Failed to update profile picture: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                if (swipeRefreshLayout != null) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload image", e);
                    Toast.makeText(getContext(),
                            "Failed to upload image: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnCompleteListener(task -> {
                    // Re-enable the edit button
                    if (getView() != null) {
                        getView().findViewById(R.id.edit_profile_image).setEnabled(true);
                    }
                });
    }

    private void updateProfileImage(String imageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Update the ImageView
        if (profileImage != null && getContext() != null) {
            Glide.with(getContext())
                    .load(imageUrl)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable disk caching
                    .skipMemoryCache(false) // Enable memory caching
                    .into(profileImage);

            // Stop the refresh animation if it's running
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    private void loadProfileImage() {
        if (currentUser == null) {
            Log.w("ProfilePage", "No user logged in.");
            return;
        }

        String userEmail = currentUser.getEmail();
        if (userEmail == null) {
            Log.w("ProfilePage", "User email not available.");
            return;
        }

        // Load profile image from Firestore
        db.collection("users").document(userEmail).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        String imageUrl = document.getString("profileImage");

                        if (imageUrl == null && currentUser.getPhotoUrl() != null) {
                            imageUrl = currentUser.getPhotoUrl().toString();
                        }

                        if (imageUrl != null) {
                            displayProfileImage(imageUrl);
                        } else {
                            Log.w("ProfilePage", "No profile image found for user.");
                        }
                    } else {
                        Log.e("ProfilePage", "Error fetching profile image", task.getException());
                    }
                });
    }

    private void displayProfileImage(String imageUrl) {
        if (getContext() != null && profileImage != null) {
            Glide.with(getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.profile) // Default placeholder
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(profileImage);
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void refreshProfile() {
        loadProfileImage();
    }
    private void storeProfileImageInFirestore(String imageUrl) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userEmail = currentUser.getEmail();

        // Update Firestore with the profile image URL
        db.collection("users").document(userEmail)
                .update("profileImage", imageUrl)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Profile image URL stored in Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to store profile image URL in Firestore", e));
    }

    private void updateDonationsProfileImage(String newProfileImageUrl, String ownerUsername) {
        // Get reference to Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Query all donations by this user
        db.collection("allDonationItems")
                .whereEqualTo("ownerUsername", ownerUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Batch write to update all documents
                    WriteBatch batch = db.batch();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        batch.update(document.getReference(),
                                "ownerProfileImageUrl", newProfileImageUrl);
                    }

                    // Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successfully updated all donations with new profile image");

                                // Notify any active FoodItemDetailFragment instances to refresh
                                if (getActivity() != null) {
                                    // Create an intent to broadcast the profile image update
                                    Intent intent = new Intent("profile.image.updated");
                                    intent.putExtra("newProfileImageUrl", newProfileImageUrl);
                                    intent.putExtra("ownerUsername", ownerUsername);

                                    // Send local broadcast
                                    LocalBroadcastManager.getInstance(getActivity())
                                            .sendBroadcast(intent);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating donations with new profile image", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying donations", e);
                });
    }

    private void setupProfileImageClick() {
        if (profileImage != null) {
            profileImage.setOnClickListener(v -> {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    String userEmail = currentUser.getEmail();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    // Retrieve profile image URL from Firestore
                    db.collection("users").document(userEmail).get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    DocumentSnapshot document = task.getResult();
                                    String imageUrl = document.getString("profileImage");

                                    // Fallback to Firebase Auth photo URL if Firestore doesn't have it
                                    if (imageUrl == null && currentUser.getPhotoUrl() != null) {
                                        imageUrl = currentUser.getPhotoUrl().toString();
                                    }

                                    // Create and show FullScreenImageFragment
                                    FullScreenImageFragment fullScreenFragment =
                                            FullScreenImageFragment.newInstance(imageUrl, R.drawable.profile);
                                    requireActivity().getSupportFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.fragment_container, fullScreenFragment)
                                            .addToBackStack(null)
                                            .commit();
                                } else {
                                    Log.e("ProfileImageClick", "Error fetching profile image", task.getException());
                                }
                            });
                }
            });
        }
    }

    private void openHistory(String type) {
        UserHistoryFragment historyFragment = UserHistoryFragment.newInstance(type);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, historyFragment)
                .addToBackStack(null)
                .commit();
    }

    private void setupClickListeners(View view) {
        // Set up sign out button
        if (signOutButton != null) {
            signOutButton.setOnClickListener(v -> signOut());
        }

        // Set up SwipeRefreshLayout colors
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(
                R.color.button_green,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            );
        }

        // Initialize other views
        editProfileButton = view.findViewById(R.id.editProfileButton);
        rateAppButton = view.findViewById(R.id.rate_app_button);
        termsConditionButton = view.findViewById(R.id.terms_conditions_button);
        donatedCountTV = view.findViewById(R.id.donated_count);
        requestedCountTV = view.findViewById(R.id.requested_count);
        campaignsCountTV = view.findViewById(R.id.campaign_count);
        volunteerCountTV = view.findViewById(R.id.volunteer_count);
        notificationIV = view.findViewById(R.id.menu_icon2);
        resetPass = view.findViewById(R.id.resetPassButton);

        // Set up reset password
        setResetPassClickListener();

        // Set up notification click
        if (notificationIV != null) {
            notificationIV.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new NotificationAll())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // Set up edit profile click
        if (editProfileButton != null) {
            editProfileButton.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new EditProfile())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // Set up rate app click
        if (rateAppButton != null) {
            rateAppButton.setOnClickListener(v -> {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null && currentUser.getEmail() != null) {
                    RateUIPage rateUIPage = new RateUIPage(getActivity(), currentUser.getEmail());
                    rateUIPage.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
                    rateUIPage.setCancelable(false);
                    rateUIPage.show();
                } else {
                    Toast.makeText(getContext(), "Please sign in to rate", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Set up terms and conditions click
        if (termsConditionButton != null) {
            termsConditionButton.setOnClickListener(v -> {
                String termsUrl = "https://sites.google.com/view/shareplate-terms-conditions";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl));
                startActivity(browserIntent);
            });
        }

        // Set up history section clicks
        View donatedSection = view.findViewById(R.id.food_donated);
        View requestedSection = view.findViewById(R.id.food_requested);
        View campaignSection = view.findViewById(R.id.campaign_volunteer);
        View volunteerSection = view.findViewById(R.id.volunteer_section);

        if (donatedSection != null) donatedSection.setOnClickListener(v -> openHistory("donated"));
        if (requestedSection != null) requestedSection.setOnClickListener(v -> openHistory("requested"));
        if (campaignSection != null) campaignSection.setOnClickListener(v -> openHistory("campaigns"));
        if (volunteerSection != null) volunteerSection.setOnClickListener(v -> openHistory("volunteering"));

        MaterialButton personalInfoButton = view.findViewById(R.id.personalInfoButton);
        if (personalInfoButton != null) {
            personalInfoButton.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new PersonalInfoFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }
    }

}