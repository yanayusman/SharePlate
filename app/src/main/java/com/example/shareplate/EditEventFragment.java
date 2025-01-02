package com.example.shareplate;

import static android.content.ContentValues.TAG;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageReference;

public class EditEventFragment extends HostActivityFragment {
    private static final String ARG_EVENT_ITEM = "event_item";
    private String eventType = "";
    private Event event;

    public static EditEventFragment newInstance(Event item) {
        EditEventFragment fragment = new EditEventFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize progress bar
        progressBar = view.findViewById(R.id.progress_bar);

        // Change title and button text
        if (getActivity() != null) {
            getActivity().setTitle("Edit Event");
        }
        campaignButton.setOnClickListener(v -> selectActivityType("Campaigns", campaignButton, volunteerButton));
        volunteerButton.setOnClickListener(v -> selectActivityType("Volunteering", volunteerButton, campaignButton));
        submitButton.setText("Update Event");

        // Get donation item from arguments
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable(ARG_EVENT_ITEM);
            if (event != null) {
                // Pre-fill the form with existing data
                eventNameInput.setText(event.getName());
                eventDescriptionInput.setText(event.getDescription());
                eventTimeInput.setText(event.getTime());
                eventDateInput.setText(event.getDate());
                eventLocationInput.setText(event.getLocation());
                eventSeatsAvailableInput.setText(event.getLocation());

                // Set existing type of event and update button colors
                if ("Campaigns".equals(event.getTypeOfEvents())) {
                    selectActivityType("Campaigns", campaignButton, volunteerButton);
                } else if ("Volunteering".equals(event.getTypeOfEvents())) {
                    selectActivityType("Volunteering", volunteerButton, campaignButton);
                }

                // Load existing image if available
                if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                    Glide.with(requireContext())
                            .load(event.getImageUrl())
                            .into(foodImageView);
                }
            }
        }
    }

    private void selectActivityType(String type, Button selectedButton, Button otherButton) {
        eventType = type;
        selectedButton.setBackgroundColor(Color.GREEN);
        otherButton.setBackgroundColor(Color.LTGRAY);
    }

    protected void submitEvent(String currentUserEmail) {
        if (!validateInputs()) return;

        // Show loading indicator
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        submitButton.setEnabled(false);

        // Create updated donation data
        Event updatedItem = new Event(
                eventNameInput.getText().toString(),
                eventDescriptionInput.getText().toString(),
                eventDateInput.getText().toString(),
                eventTimeInput.getText().toString(),
                eventType,
                eventSeatsAvailableInput.getText().toString(),
                eventLocationInput.getText().toString(),
                event.getImageResourceId(),
                event.getImageUrl(),
                event.getOwnerProfileImageUrl(),
                currentUserEmail,
                event.getOwnerUsername()
        );


        updatedItem.setDocumentId(event.getDocumentId());
        updatedItem.setCreatedAt(event.getCreatedAt());

        // If a new image was selected, upload it first
        if (selectedImageUri != null) {
            String imageFileName = "food_images/" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = storageRef.child(imageFileName);

            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            imageRef.getDownloadUrl()
                                    .addOnSuccessListener(downloadUri -> {
                                        updatedItem.setImageUrl(downloadUri.toString());
                                        updateEventInFirestore(updatedItem);
                                    })
                                    .addOnFailureListener(this::handleError))
                    .addOnFailureListener(this::handleError);
        } else {
            // Update without changing the image
            updateEventInFirestore(updatedItem);
        }
    }

    private void updateEventInFirestore(Event updatedItem) {
        if (updatedItem.getDocumentId() == null || updatedItem.getDocumentId().isEmpty()) {
            Log.e(TAG, "Document ID is null or empty, cannot update the event.");
            Toast.makeText(getContext(), "Error: Event ID is missing", Toast.LENGTH_SHORT).show();
            return; // Exit the method early if the documentId is invalid
        }

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(updatedItem.getDocumentId())
                .set(updatedItem)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Event updated successfully", Toast.LENGTH_SHORT).show();
                    }
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    submitButton.setEnabled(true);
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(this::handleError);
    }


    private void handleError(Exception e) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        submitButton.setEnabled(true);
    }
}