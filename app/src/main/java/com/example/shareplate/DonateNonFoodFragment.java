package com.example.shareplate;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.net.Uri;
import android.content.Intent;
import android.provider.MediaStore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import android.widget.ProgressBar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DonateNonFoodFragment extends Fragment {
    protected EditText nameInput, categoryInput, descriptionInput, quantityInput, pickupTimeInput, locationInput;
    protected Button submitButton;
    private ImageView backButton;
    private NonFoodItemRepository nonFoodItemRepository;
    private Calendar timeCalendar;
    private SimpleDateFormat timeFormatter;
    private ImageView itemImageView;
    protected ImageView foodImageView;
    protected Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private FirebaseStorage storage;
    protected StorageReference storageRef;
    protected ProgressBar progressBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nonFoodItemRepository = new NonFoodItemRepository();
        timeCalendar = Calendar.getInstance();
        timeFormatter = new SimpleDateFormat("hh:mm a", Locale.US); // 12-hour format with AM/PM

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        // Show selected image
                        Glide.with(this)
                                .load(selectedImageUri)
                                .centerCrop()
                                .into(itemImageView);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donate_non_food, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        initializeViews(view);

        // Set up click listeners
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        submitButton.setOnClickListener(v -> submitDonation());

        // Set up time picker
        pickupTimeInput.setOnClickListener(v -> showTimePicker());
        pickupTimeInput.setFocusable(false);
    }

    private void initializeViews(View view) {
        nameInput = view.findViewById(R.id.name_input);
        categoryInput = view.findViewById(R.id.food_category_input);
        descriptionInput = view.findViewById(R.id.description_input);
        quantityInput = view.findViewById(R.id.quantity_input);
        pickupTimeInput = view.findViewById(R.id.pickup_time_input);
        locationInput = view.findViewById(R.id.event_seats_available_input);
        submitButton = view.findViewById(R.id.submit_button);
        backButton = view.findViewById(R.id.back_button);
        itemImageView = view.findViewById(R.id.food_image);
        Button uploadImageButton = view.findViewById(R.id.upload_image_button);

        uploadImageButton.setOnClickListener(v -> openImagePicker());
    }

    protected void submitDonation() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        View view = getView();
        if (view == null) {
            Toast.makeText(getContext(), "Error: View not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator
        progressBar = view.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        if (selectedImageUri != null) {
            // Upload image first
            String imageFileName = "food_images/" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = storageRef.child(imageFileName);

            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get download URL
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    // Create and save donation with image URL
                                    saveDonationWithImage(downloadUri.toString());
                                    progressBar.setVisibility(View.GONE);
                                    submitButton.setEnabled(true);
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    submitButton.setEnabled(true);
                                    Toast.makeText(getContext(),
                                            "Failed to get image URL: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        submitButton.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Failed to upload image: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Save donation without image
            saveDonationWithImage(null);
            progressBar.setVisibility(View.GONE);
            submitButton.setEnabled(true);
        }
    }

    private void saveDonationWithImage(String imageUrl) {
        try {
            // Get current user
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String ownerEmail = currentUser.getEmail();
            String ownerProfileImageUrl = currentUser.getPhotoUrl() != null ?
                    currentUser.getPhotoUrl().toString() : "";

            // Format the data
            String name = nameInput.getText().toString().trim();
            String category = categoryInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            String quantity = quantityInput.getText().toString().trim();
            String pickupTime = pickupTimeInput.getText().toString().trim();
            String location = locationInput.getText().toString().trim();
            String donateType = "NonFood";

            // Create new DonationItem with owner username
            NonFoodItem newDonation = new NonFoodItem(
                    name,
                    category,
                    description,
                    quantity,
                    pickupTime,
                    location,
                    R.drawable.placeholder_image,
                    imageUrl,
                    ownerEmail,
                    ownerProfileImageUrl,
                    donateType
            );

            // Add to Firebase
            nonFoodItemRepository.addNonFoodItem(newDonation);

            // Show success message and navigate back
            if (getContext() != null) {
                Toast.makeText(getContext(), "Donation submitted successfully!",
                        Toast.LENGTH_SHORT).show();
            }
            if (getActivity() != null) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(),
                        "Error saving donation: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected boolean validateInputs() {
        if (nameInput.getText().toString().trim().isEmpty()) {
            nameInput.setError("Name is required");
            return false;
        }
        if (categoryInput.getText().toString().trim().isEmpty()) {
            categoryInput.setError("Item category is required");
            return false;
        }
        if (descriptionInput.getText().toString().trim().isEmpty()) {
            descriptionInput.setError("Description is required");
            return false;
        }
        if (quantityInput.getText().toString().trim().isEmpty()) {
            quantityInput.setError("Quantity is required");
            return false;
        }
        if (pickupTimeInput.getText().toString().trim().isEmpty()) {
            pickupTimeInput.setError("Pickup time is required");
            return false;
        }
        if (locationInput.getText().toString().trim().isEmpty()) {
            locationInput.setError("Location is required");
            return false;
        }
        return true;
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                R.style.CustomPickerTheme,
                (view, hourOfDay, minute) -> {
                    timeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    timeCalendar.set(Calendar.MINUTE, minute);
                    updateTimeLabel();
                },
                timeCalendar.get(Calendar.HOUR_OF_DAY),
                timeCalendar.get(Calendar.MINUTE),
                false
        );

        timePickerDialog.show();
    }

    private void updateTimeLabel() {
        pickupTimeInput.setText(timeFormatter.format(timeCalendar.getTime()));
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }
}