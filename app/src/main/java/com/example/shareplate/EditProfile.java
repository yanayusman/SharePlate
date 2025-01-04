package com.example.shareplate;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditProfile extends Fragment {

    private EditText usernameEditText, emailEditText, phoneNumberEditText, passwordEditText, locationDisplay;
    private Button updateButton;
    private ImageView backButton, selectLocationButton, profileImage, editProfileImage;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    // private ActivityResultLauncher<Intent> imagePickerLauncher;
    private String userEmail;
    private SwipeRefreshLayout swipeRefreshLayout;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the fragment layout
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        usernameEditText = view.findViewById(R.id.username);
        emailEditText = view.findViewById(R.id.email);
        phoneNumberEditText = view.findViewById(R.id.phone_number);
        passwordEditText = view.findViewById(R.id.password);
        locationDisplay = view.findViewById(R.id.location_display);
        selectLocationButton = view.findViewById(R.id.select_location_button);
        updateButton = view.findViewById(R.id.update_button);
        backButton = view.findViewById(R.id.backBtn);
        profileImage = view.findViewById(R.id.profile_image); // Profile image
        editProfileImage = view.findViewById(R.id.edit_profile_image); // Edit icon

        // Get the current user
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
            emailEditText.setText(userEmail);
            emailEditText.setEnabled(false);
        } else {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load user profile data from Firestore
        loadUserProfile();

        editProfileImage.setOnClickListener(v -> openImagePicker());

        selectLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLocationPicker();
            }
        });
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        // Set up the update button
        updateButton.setOnClickListener(v -> updateUserProfile());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> locationPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    // Handle the selected location here
                }
            }
    );

    private void openLocationPicker() {
        if (getActivity() == null) {
            Log.e("EditProfile", "Activity is null, cannot open location picker.");
            return;
        }

        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
                .build(requireActivity());

        locationPickerLauncher.launch(intent);
    }


    // Initialize the image picker launcher
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    uploadProfileImage(selectedImageUri);
                }
            }
    );

    private void uploadProfileImage(Uri imageUri) {
        if (userEmail == null) {
            Toast.makeText(getContext(), "User email not available!", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference profileImageRef = storageRef.child("profile_images/" + userEmail + ".jpg");
        profileImageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Update the profile image in Firestore
                    db.collection("users").document(userEmail)
                            .update("profileImageUrl", uri.toString())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Profile image updated!", Toast.LENGTH_SHORT).show();
                                profileImage.setImageURI(imageUri); // Update the ImageView
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to update Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

                        usernameEditText.setText(username != null ? username : "");
                        phoneNumberEditText.setText(phoneNumber != null ? phoneNumber : "");
                        locationDisplay.setText(location != null ? location : "");
                    } else {
                        Toast.makeText(getContext(), "No profile data found. Please update your details.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load user details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    private void updateUserProfile() {
        // Retrieve updated data
        String username = usernameEditText.getText().toString().trim();
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        String newPassword = passwordEditText.getText().toString().trim();
        String location = locationDisplay.getText().toString().trim();

        // Validate input fields
        if (username.isEmpty()) {
            usernameEditText.setError("Username cannot be empty");
            return;
        }

        // Prepare updates for Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("phoneNumber", phoneNumber);
        if (location == null || location.trim().isEmpty()) {
            updates.put("location", "");
        } else {
            updates.put("location", location);
        }

        // Check if the document exists
        db.collection("users").document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Update existing document
                        db.collection("users").document(userEmail)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                    requireActivity().getSupportFragmentManager().popBackStack();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // Create a new document if it doesn't exist
                        updates.put("email", userEmail); // Add email to the document
                        db.collection("users").document(userEmail)
                                .set(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Profile created successfully!", Toast.LENGTH_SHORT).show();
                                    requireActivity().getSupportFragmentManager().popBackStack();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to create profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to fetch user details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Update password in FirebaseAuth if provided
        if (!newPassword.isEmpty()) {
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                currentUser.updatePassword(newPassword)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }



}