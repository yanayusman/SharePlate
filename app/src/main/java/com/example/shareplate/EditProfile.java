package com.example.shareplate;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditProfile extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private EditText usernameEditText, emailEditText, phoneNumberEditText, passwordEditText, locationDisplay;
    private Button updateButton;
    private ImageView backButton, selectLocationButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userEmail;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;

    private FusedLocationProviderClient fusedLocationProviderClient;

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
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());

        usernameEditText = view.findViewById(R.id.username);
        emailEditText = view.findViewById(R.id.email);
        phoneNumberEditText = view.findViewById(R.id.phone_number);
        passwordEditText = view.findViewById(R.id.password);
        locationDisplay = view.findViewById(R.id.location_display);
        selectLocationButton = view.findViewById(R.id.select_location_button);
        updateButton = view.findViewById(R.id.update_button);
        backButton = view.findViewById(R.id.backBtn);

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

        selectLocationButton.setOnClickListener(v -> {
            openMapFragment();
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 99);
            }
        });

        // Set up the update button
        updateButton.setOnClickListener(v -> updateUserProfile());
    }

    private void openMapFragment() {
        // Load the MapsFragment into the container
        SupportMapFragment mapFragment = new SupportMapFragment();

        // Pass the map fragment dynamically
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.maps_container, mapFragment)
                .addToBackStack(null)
                .commit();

        // Make the container visible
        View mapsContainer = getView().findViewById(R.id.maps_container);
        if (mapsContainer != null) {
            mapsContainer.setVisibility(View.VISIBLE);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 99);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
            @Override
                public void onSuccess(Location location) {
                    // Check if the location is not null
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        // Use Geocoder to convert coordinates into address
                        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                            if (addresses != null && !addresses.isEmpty()) {
                                // Get the first address
                                String address = addresses.get(0).getAddressLine(0);
                                locationDisplay.setText(address);  // Display the address
                            } else {
                                locationDisplay.setText("No address found.");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            locationDisplay.setText("Error in geocoding.");
                        }
                    } else {
                        locationDisplay.setText("Unable to get current location.");
                    }
                }
            });
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