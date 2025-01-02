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

public class EditRequestFragment extends DonateItemFragment {
    private static final String ARG_DONATION_ITEM = "request_item";
//    private RequestFood requestFood;
//
    public static EditRequestFragment newInstance(RequestFood item) {
        EditRequestFragment fragment = new EditRequestFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DONATION_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        // Initialize progress bar
//        progressBar = view.findViewById(R.id.progress_bar);
//
//        // Change title and button text
//        if (getActivity() != null) {
//            getActivity().setTitle("Edit Request");
//        }
//        submitButton.setText("Update Donation");
//
//        // Get donation item from arguments
//        if (getArguments() != null) {
//            requestFood = (RequestFood) getArguments().getSerializable(ARG_DONATION_ITEM);
//            if (requestFood != null) {
//                // Pre-fill the form with existing data
//                nameInput.setText(requestFood.getName());
//                foodCategoryInput.setText(requestFood.getFoodCategory());
//                expiryDateInput.setText(requestFood.getUrgencyLevel());
//                quantityInput.setText(requestFood.getQuantity());
//                pickupTimeInput.setText(requestFood.getPickupTime());
//                locationInput.setText(requestFood.getLocation());
//
//                // Load existing image if available
//                if (requestFood.getImageUrl() != null && !requestFood.getImageUrl().isEmpty()) {
//                    Glide.with(requireContext())
//                            .load(requestFood.getImageUrl())
//                            .into(foodImageView);
//                }
//            }
//        }
//    }
//
//    @Override
//    protected void submitDonation() {
//        if (!validateInputs()) return;
//
//        // Show loading indicator
//        if (progressBar != null) {
//            progressBar.setVisibility(View.VISIBLE);
//        }
//        submitButton.setEnabled(false);
//
//        // Create updated donation data
//        DonationItem updatedItem = new DonationItem(
//                nameInput.getText().toString(),
//                foodCategoryInput.getText().toString(),
//                "",
//                "",
//                expiryDateInput.getText().toString(),
//                quantityInput.getText().toString(),
//                pickupTimeInput.getText().toString(),
//                locationInput.getText().toString(),
//                R.drawable.placeholder_image,
//                requestFood.getImageUrl(),
//                requestFood.getOwnerUsername(),
//                requestFood.getDonateType(),
//                requestFood.getOwnerProfileImageUrl()
//        );
//        updatedItem.setDocumentId(requestFood.getDocumentId());
//        updatedItem.setStatus(requestFood.getStatus());
//        updatedItem.setCreatedAt(requestFood.getCreatedAt());
//
//        // If a new image was selected, upload it first
//        if (selectedImageUri != null) {
//            String imageFileName = "food_images/" + System.currentTimeMillis() + ".jpg";
//            StorageReference imageRef = storageRef.child(imageFileName);
//
//            imageRef.putFile(selectedImageUri)
//                    .addOnSuccessListener(taskSnapshot ->
//                            imageRef.getDownloadUrl()
//                                    .addOnSuccessListener(downloadUri -> {
//                                        updatedItem.setImageUrl(downloadUri.toString());
//                                        updateDonationInFirestore(updatedItem);
//                                    })
//                                    .addOnFailureListener(this::handleError))
//                    .addOnFailureListener(this::handleError);
//        } else {
//            // Update without changing the image
//            updateDonationInFirestore(updatedItem);
//        }
//    }
//
//    private void updateDonationInFirestore(DonationItem updatedItem) {
//        FirebaseFirestore.getInstance()
//                .collection("foodRequest")
//                .document(requestFood.getDocumentId())
//                .set(updatedItem)  // Use set instead of update to ensure all fields are updated
//                .addOnSuccessListener(aVoid -> {
//                    if (getContext() != null) {
//                        Toast.makeText(getContext(), "Request updated successfully", Toast.LENGTH_SHORT).show();
//                    }
//                    if (progressBar != null) {
//                        progressBar.setVisibility(View.GONE);
//                    }
//                    submitButton.setEnabled(true);
//                    requireActivity().getSupportFragmentManager().popBackStack();
//                })
//                .addOnFailureListener(this::handleError);
//    }
//
//    private void handleError(Exception e) {
//        if (getContext() != null) {
//            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//        if (progressBar != null) {
//            progressBar.setVisibility(View.GONE);
//        }
//        submitButton.setEnabled(true);
//    }
}