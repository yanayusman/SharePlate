package com.example.shareplate;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HostActivityFragment extends Fragment {
    protected EditText eventNameInput, eventDescriptionInput, eventTimeInput, eventDateInput, eventLocationInput, eventSeatsAvailableInput;
    protected Button campaignButton, volunteerButton, submitButton;
    protected ProgressBar progressBar;
    private String eventType = "";
    private ImageView backButton;
    private EventRepo eventRepo;
    private Calendar calendar;
    private SimpleDateFormat dateFormatter;
    private Calendar timeCalendar;
    private SimpleDateFormat timeFormatter;
    protected ImageView foodImageView;
    protected Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private FirebaseStorage storage;
    protected StorageReference storageRef;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri photoUri;
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventRepo = new EventRepo();
        calendar = Calendar.getInstance();
        timeCalendar = Calendar.getInstance();
        dateFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        timeFormatter = new SimpleDateFormat("hh:mm a", Locale.US);

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
                                .into(foodImageView);
                    }
                }
        );

        // Initialize camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (photoUri != null) {
                            selectedImageUri = photoUri;
                            // Show selected image
                            Glide.with(this)
                                    .load(photoUri)
                                    .centerCrop()
                                    .into(foodImageView);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_host_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in to donate", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUserEmail == null || currentUserEmail.isEmpty()) {
            Toast.makeText(getContext(), "Error: No email found for the current user.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize views
        initializeViews(view);

        // Set up click listeners
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        campaignButton.setOnClickListener(v -> selectActivityType("Campaigns", campaignButton, volunteerButton));
        volunteerButton.setOnClickListener(v -> selectActivityType("Volunteering", volunteerButton, campaignButton));
        submitButton.setOnClickListener(v -> submitEvent(currentUserEmail));

        // Set up date picker
        eventDateInput.setOnClickListener(v -> showDatePicker());
        eventDateInput.setFocusable(false);

        // Set up time picker
        eventTimeInput.setOnClickListener(v -> showTimePicker());
        eventTimeInput.setFocusable(false);
    }

    private void selectActivityType(String type, Button selectedButton, Button otherButton) {
        eventType = type;
        selectedButton.setBackgroundColor(Color.GREEN);
        otherButton.setBackgroundColor(Color.LTGRAY);
    }

    private void initializeViews(View view){
        eventNameInput = view.findViewById(R.id.event_name_input);
        eventDescriptionInput = view.findViewById(R.id.event_description_input);
        eventTimeInput = view.findViewById(R.id.event_time_input);
        eventDateInput = view.findViewById(R.id.event_date_input);
        eventLocationInput = view.findViewById(R.id.event_location_input);
        eventSeatsAvailableInput = view.findViewById(R.id.event_seats_available_input);
        campaignButton = view.findViewById(R.id.event_campaigns_button);
        volunteerButton = view.findViewById(R.id.event_volunteer_button);
        submitButton = view.findViewById(R.id.submit_button);
        backButton = view.findViewById(R.id.back_button);
        foodImageView = view.findViewById(R.id.food_image);

        Button uploadImageButton = view.findViewById(R.id.upload_image_button);
        uploadImageButton.setOnClickListener(v -> showImageSourceDialog());
    }

    protected void submitEvent(String email) {
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
                                    saveEventWithImage(downloadUri.toString(), email);
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
            saveEventWithImage(null, email);
            progressBar.setVisibility(View.GONE);
            submitButton.setEnabled(true);
        }
    }

    private void saveEventWithImage(String imageUrl, String currentUserEmail) {
        String name = eventNameInput.getText().toString();
        String description = eventDescriptionInput.getText().toString();
        String time = eventTimeInput.getText().toString();
        String date = eventDateInput.getText().toString();
        String location = eventLocationInput.getText().toString();
        String seatsAvailable = eventSeatsAvailableInput.getText().toString();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUserEmail == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String ownerProfileImageUrl = currentUser.getPhotoUrl() != null ?
                currentUser.getPhotoUrl().toString() : "";

        // Fetch the username from Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("email", currentUserEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String username = querySnapshot.getDocuments().get(0).getString("username");

                        // Create the event with the retrieved username
                        Event newEvent = new Event(
                                name,
                                description,
                                date,
                                time,
                                eventType,
                                seatsAvailable,
                                location,
                                R.drawable.placeholder_image,
                                imageUrl,
                                ownerProfileImageUrl,
                                currentUserEmail,
                                username
                        );

                        // Save the event
                        EventRepo repository = new EventRepo();
                        repository.addEvent(currentUserEmail, newEvent, new EventRepo.OnEventCompleteListener() {
                            @Override
                            public void onEventSuccess() {
                                if (getContext() != null) {
                                    newEvent.setDocumentId(newEvent.getDocumentId());
                                    Toast.makeText(getContext(), "Event added successfully", Toast.LENGTH_SHORT).show();
                                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                                    // Fetch the username from the users collection
                                    db.collection("users")
                                            .whereEqualTo("email", currentUserEmail)
                                            .get()
                                            .addOnSuccessListener(querySnapshot -> {
                                                String ownersUsername = "Unknown User";
                                                if (!querySnapshot.isEmpty()) {
                                                    ownersUsername = querySnapshot.getDocuments().get(0).getString("username");
                                                }

                                                Map<String, Object> notificationData = new HashMap<>();
                                                notificationData.put("ownerEmail", currentUserEmail);
                                                notificationData.put("itemName", name);
                                                notificationData.put("location", location);
                                                notificationData.put("imageUrl", imageUrl);
                                                notificationData.put("expiredDate", date);
                                                notificationData.put("status", "unread");
                                                notificationData.put("message", ownersUsername + " has a new event!");
                                                notificationData.put("activityType", "event");

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
                                                notificationData.put("ownerEmail", currentUserEmail);
                                                notificationData.put("itemName", name);
                                                notificationData.put("location", location);
                                                notificationData.put("imageUrl", imageUrl);
                                                notificationData.put("expiredDate", date);
                                                notificationData.put("status", "unread");
                                                notificationData.put("message", currentUserEmail + " has a new event!");
                                                notificationData.put("activityType", "event");

                                                db.collection("notifications")
                                                        .add(notificationData)
                                                        .addOnSuccessListener(documentReference -> {
                                                            Log.d("Notification", "Notification stored successfully (fallback to email)");
                                                        })
                                                        .addOnFailureListener(err -> {
                                                            Log.e("NotificationError", "Failed to store notification (fallback): " + err.getMessage());
                                                        });
                                            });
                                    // Clear form or navigate back
                                    requireActivity().getSupportFragmentManager().popBackStack();
                                }
                            }

                            @Override
                            public void onEventFailure(Exception e) {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(),
                                            "Failed to add event: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    } else {
                        Toast.makeText(getContext(), "Username not found for the given email.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to retrieve username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    protected boolean validateInputs() {
        if (eventNameInput.getText().toString().trim().isEmpty()) {
            eventNameInput.setError("Name is required");
            return false;
        }
        if (eventDescriptionInput.getText().toString().trim().isEmpty()) {
            eventDescriptionInput.setError("Food category is required");
            return false;
        }
        if (eventDateInput.getText().toString().trim().isEmpty()) {
            eventDateInput.setError("Expiry date is required");
            return false;
        }
        if (eventTimeInput.getText().toString().trim().isEmpty()) {
            eventTimeInput.setError("Quantity is required");
            return false;
        }
        if (eventSeatsAvailableInput.getText().toString().trim().isEmpty()) {
            eventSeatsAvailableInput.setError("Pickup time is required");
            return false;
        }
        if (eventLocationInput.getText().toString().trim().isEmpty()) {
            eventLocationInput.setError("Location is required");
            return false;
        }
        return true;
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                R.style.CustomPickerTheme,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateExpiryDateLabel();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date as today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());

        datePickerDialog.show();
    }

    private void updateExpiryDateLabel() {
        eventDateInput.setText(dateFormatter.format(calendar.getTime()));
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
        eventTimeInput.setText(timeFormatter.format(timeCalendar.getTime()));
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Take photo with camera
                        if (checkCameraPermission()) {
                            openCamera();
                        } else {
                            requestCameraPermission();
                        }
                    } else {
                        // Choose from gallery
                        openImagePicker();
                    }
                })
                .show();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getContext(),
                        "Error creating image file", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(requireContext(),
                        "com.shareplate.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",        /* suffix */
                storageDir     /* directory */
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(getContext(),
                        "Camera permission is required to take photos",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
