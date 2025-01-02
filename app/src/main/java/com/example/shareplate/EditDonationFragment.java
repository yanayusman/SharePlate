package com.example.shareplate;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageReference;

public class EditDonationFragment extends DonateItemFragment {
    private static final String ARG_DONATION_ITEM = "donation_item";
    private DonationItem donationItem;

    public static EditDonationFragment newInstance(DonationItem item) {
        EditDonationFragment fragment = new EditDonationFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DONATION_ITEM, item);
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
            getActivity().setTitle("Edit Donation");
        }
        submitButton.setText("Update Donation");

        // Get donation item from arguments
        if (getArguments() != null) {
            donationItem = (DonationItem) getArguments().getSerializable(ARG_DONATION_ITEM);
            if (donationItem != null) {
                // Pre-fill the form with existing data
                nameInput.setText(donationItem.getName());
                foodCategoryInput.setText(donationItem.getFoodCategory());
                expiryDateInput.setText(donationItem.getExpiredDate());
                quantityInput.setText(donationItem.getQuantity());
                pickupTimeInput.setText(donationItem.getPickupTime());
                locationInput.setText(donationItem.getLocation());

                // Load existing image if available
                if (donationItem.getImageUrl() != null && !donationItem.getImageUrl().isEmpty()) {
                    Glide.with(requireContext())
                            .load(donationItem.getImageUrl())
                            .into(foodImageView);
                }
            }
        }
    }

    @Override
    protected void submitDonation() {
        if (!validateInputs()) return;

        // Show loading indicator
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        submitButton.setEnabled(false);

        // Create updated donation data
        DonationItem updatedItem = new DonationItem(
                nameInput.getText().toString(),
                foodCategoryInput.getText().toString(),
                "",
                "",
                expiryDateInput.getText().toString(),
                quantityInput.getText().toString(),
                pickupTimeInput.getText().toString(),
                locationInput.getText().toString(),
                R.drawable.placeholder_image,
                donationItem.getImageUrl(),
                donationItem.getDonateType(),
                donationItem.getOwnerProfileImageUrl(),
                donationItem.getEmail()

        );
        updatedItem.setDocumentId(donationItem.getDocumentId());
        updatedItem.setStatus(donationItem.getStatus());
        updatedItem.setCreatedAt(donationItem.getCreatedAt());

        // If a new image was selected, upload it first
        if (selectedImageUri != null) {
            String imageFileName = "food_images/" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = storageRef.child(imageFileName);

            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            imageRef.getDownloadUrl()
                                    .addOnSuccessListener(downloadUri -> {
                                        updatedItem.setImageUrl(downloadUri.toString());
                                        updateDonationInFirestore(updatedItem);
                                    })
                                    .addOnFailureListener(this::handleError))
                    .addOnFailureListener(this::handleError);
        } else {
            // Update without changing the image
            updateDonationInFirestore(updatedItem);
        }
    }

    private void updateDonationInFirestore(DonationItem updatedItem) {
        FirebaseFirestore.getInstance()
                .collection("allDonationItems")
                .document(donationItem.getDocumentId())
                .set(updatedItem)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Donation updated successfully", Toast.LENGTH_SHORT).show();
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