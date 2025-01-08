package com.example.shareplate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditProfile extends Fragment {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private EditText usernameEditText;
    private EditText emailEditText;
    private EditText phoneNumberEditText;
    private TextView locationDisplay;
    private ImageView backButton, profileImage, editProfileImage, selectLocationButton;

    private Button updateButton;
    private ProgressBar loadingSpinner;

    private String userEmail;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private LocationHelper locationHelper;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // Initialize views
        initializeViews(view);

        // Initialize location helper
        initializeLocationHelper();

        // Initialize image picker
        initializeImagePicker();

        // Load existing profile data
        loadUserProfile();

        return view;
    }

    private void initializeViews(View view) {
        usernameEditText = view.findViewById(R.id.usernameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText);
//        passwordEditText = view.findViewById(R.id.password);
        locationDisplay = view.findViewById(R.id.locationDisplay);
        selectLocationButton = view.findViewById(R.id.selectLocationButton);
        updateButton = view.findViewById(R.id.updateProfileButton);
        backButton = view.findViewById(R.id.backBtn);
        profileImage = view.findViewById(R.id.profile_image); // Profile image
        editProfileImage = view.findViewById(R.id.edit_profile_image); // Edit icon


        // Set email field to be non-editable
        emailEditText.setText(userEmail);
        emailEditText.setEnabled(false); // This makes it read-only
        // Optional: Change the appearance to indicate it's non-editable
        emailEditText.setBackgroundResource(android.R.color.transparent);
        emailEditText.setTextColor(getResources().getColor(android.R.color.darker_gray));

        // Set click listeners
        selectLocationButton.setOnClickListener(v -> checkLocationSettings());
        updateButton.setOnClickListener(v -> updateUserProfile());
        profileImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
    }

    private void initializeLocationHelper() {
        locationHelper = new LocationHelper(requireContext(), new LocationHelper.LocationResultListener() {
            @Override
            public void onLocationResult(Location location) {
                processLocation(location);
            }

            @Override
            public void onLocationError(String error) {
                Log.e("Location", error);
                requireActivity().runOnUiThread(() -> showManualLocationInput());
            }
        });
    }

    private void initializeImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadProfileImage(uri);
                    }
                }
        );
    }

    private void checkLocationSettings() {
        LocationManager locationManager = (LocationManager) requireContext()
                .getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Enable GPS")
                    .setMessage("GPS is required for accurate location. Would you like to enable it?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        showManualLocationInput();
                    })
                    .show();
            return;
        }

        // Show loading indicator
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(View.VISIBLE);
        }

        // Request location
        locationHelper.getCurrentLocation();
    }

    private void processLocation(Location location) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), new Locale("ms", "MY"));
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = formatAddress(address);

                requireActivity().runOnUiThread(() -> {
                    locationDisplay.setText(fullAddress);
                    if (loadingSpinner != null) {
                        loadingSpinner.setVisibility(View.GONE);
                    }
                    saveLocationToFirebase(fullAddress);
                });
            } else {
                requireActivity().runOnUiThread(() -> {
                    if (loadingSpinner != null) {
                        loadingSpinner.setVisibility(View.GONE);
                    }
                    showManualLocationInput();
                });
            }
        } catch (IOException e) {
            Log.e("Location", "Geocoding error: " + e.getMessage());
            requireActivity().runOnUiThread(() -> {
                if (loadingSpinner != null) {
                    loadingSpinner.setVisibility(View.GONE);
                }
                showManualLocationInput();
            });
        }
    }

    private String formatAddress(Address address) {
        StringBuilder sb = new StringBuilder();

        if (address.getThoroughfare() != null)
            sb.append(address.getThoroughfare()).append(", ");
        if (address.getSubLocality() != null)
            sb.append(address.getSubLocality()).append(", ");
        if (address.getLocality() != null)
            sb.append(address.getLocality()).append(", ");
        if (address.getPostalCode() != null)
            sb.append(address.getPostalCode()).append(" ");
        if (address.getAdminArea() != null)
            sb.append(address.getAdminArea()).append(", ");
        if (address.getCountryName() != null)
            sb.append(address.getCountryName());

        return sb.toString().trim();
    }

    private void showManualLocationInput() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Location Not Available")
                .setMessage("Unable to get accurate location. Would you like to enter your location manually?")
                .setPositiveButton("Enter Manually", (dialog, which) -> {
                    AlertDialog.Builder inputDialog = new AlertDialog.Builder(requireContext());
                    final EditText input = new EditText(requireContext());
                    input.setHint("Enter your location");
                    inputDialog.setView(input);
                    inputDialog.setPositiveButton("OK", (dialogInterface, i) -> {
                        String manualLocation = input.getText().toString().trim();
                        if (!manualLocation.isEmpty()) {
                            locationDisplay.setText(manualLocation);
                            saveLocationToFirebase(manualLocation);
                        }
                    });
                    inputDialog.setNegativeButton("Cancel", null);
                    inputDialog.show();
                })
                .setNegativeButton("Try Again", (dialog, which) -> checkLocationSettings())
                .show();
    }

    private void loadUserProfile() {
        if (userEmail == null) return;

        db.collection("users").document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate fields with data from Firestore
                        String username = documentSnapshot.getString("username");
                        String phoneNumber = documentSnapshot.getString("phoneNumber");
                        String location = documentSnapshot.getString("location");
                        String profileImageUrl = documentSnapshot.getString("profileImage");

                        usernameEditText.setText(username != null ? username : "");
                        emailEditText.setText(userEmail); // Always show the current user's email
                        phoneNumberEditText.setText(phoneNumber != null ? phoneNumber : "");
                        locationDisplay.setText(location != null ? location : "");

                        // Load profile image using Glide
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.profile)
                                    .into(profileImage);
                        } else {
                            // Use default profile image
                            profileImage.setImageResource(R.drawable.profile);
                        }
                    } else {
                        Toast.makeText(getContext(), "No profile data found. Please update your details.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load user details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserProfile() {
        // Show loading spinner
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(View.VISIBLE);
        }

        // Retrieve updated data
        String username = usernameEditText.getText().toString().trim();
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        String location = locationDisplay.getText().toString().trim();

        // Validate input fields
        if (username.isEmpty()) {
            usernameEditText.setError("Username cannot be empty");
            if (loadingSpinner != null) {
                loadingSpinner.setVisibility(View.GONE);
            }
            return;
        }

        // Prepare updates for Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("phoneNumber", phoneNumber);
        updates.put("location", location);

        // Update Firestore
        db.collection("users").document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Update existing document
                        db.collection("users").document(userEmail)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // Broadcast username update
                                    Intent usernameIntent = new Intent("profile.username.updated");
                                    usernameIntent.putExtra("newUsername", username);
                                    LocalBroadcastManager.getInstance(requireContext())
                                            .sendBroadcast(usernameIntent);

                                    Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                    if (loadingSpinner != null) {
                                        loadingSpinner.setVisibility(View.GONE);
                                    }
                                    requireActivity().getSupportFragmentManager().popBackStack();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    if (loadingSpinner != null) {
                                        loadingSpinner.setVisibility(View.GONE);
                                    }
                                });
                    } else {
                        // Create new document
                        updates.put("email", userEmail);
                        db.collection("users").document(userEmail)
                                .set(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // Broadcast username update
                                    Intent usernameIntent = new Intent("profile.username.updated");
                                    usernameIntent.putExtra("newUsername", username);
                                    LocalBroadcastManager.getInstance(requireContext())
                                            .sendBroadcast(usernameIntent);

                                    Toast.makeText(getContext(), "Profile created successfully!", Toast.LENGTH_SHORT).show();
                                    if (loadingSpinner != null) {
                                        loadingSpinner.setVisibility(View.GONE);
                                    }
                                    requireActivity().getSupportFragmentManager().popBackStack();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to create profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    if (loadingSpinner != null) {
                                        loadingSpinner.setVisibility(View.GONE);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to fetch user details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (loadingSpinner != null) {
                        loadingSpinner.setVisibility(View.GONE);
                    }
                });
    }

    private void uploadProfileImage(Uri imageUri) {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(View.VISIBLE);
        }

        // Create a reference to store the image
        StorageReference imageRef = storageRef.child("profile_images/" + userEmail + ".jpg");

        // Upload the image
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Update Firestore with the new image URL
                        db.collection("users").document(userEmail)
                                .update("profileImage", uri.toString())
                                .addOnSuccessListener(aVoid -> {
                                    // Broadcast profile image update
                                    Intent imageIntent = new Intent("profile.image.updated");
                                    imageIntent.putExtra("newProfileImageUrl", uri.toString());
                                    LocalBroadcastManager.getInstance(requireContext())
                                            .sendBroadcast(imageIntent);

                                    Toast.makeText(getContext(), "Profile image updated!", Toast.LENGTH_SHORT).show();

                                    // Update ImageView with new image
                                    Glide.with(this)
                                            .load(uri.toString())
                                            .circleCrop()
                                            .into(profileImage);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to update Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                })
                                .addOnCompleteListener(task -> {
                                    if (loadingSpinner != null) {
                                        loadingSpinner.setVisibility(View.GONE);
                                    }
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (loadingSpinner != null) {
                        loadingSpinner.setVisibility(View.GONE);
                    }
                });
    }

    private void saveLocationToFirebase(String location) {
        if (userEmail == null) return;

        db.collection("users").document(userEmail)
                .update("location", location)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "Location successfully updated!");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error updating location: " + e.getMessage());
                    Toast.makeText(requireContext(),
                            "Error saving location to database",
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get location
                checkLocationSettings();
            } else {
                Toast.makeText(requireContext(),
                        "Location permission denied. Cannot get current location.",
                        Toast.LENGTH_SHORT).show();
                showManualLocationInput();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationHelper != null) {
            locationHelper.stopLocationUpdates();
        }
    }
}