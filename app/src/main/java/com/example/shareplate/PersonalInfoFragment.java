package com.example.shareplate;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;

public class PersonalInfoFragment extends Fragment {
    private TextView usernameTV, emailTV, phoneTV, locationTV;
    private ImageView backButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal_info, container, false);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        usernameTV = view.findViewById(R.id.usernameTV);
        emailTV = view.findViewById(R.id.emailTV);
        phoneTV = view.findViewById(R.id.phoneTV);
        locationTV = view.findViewById(R.id.locationTV);
        backButton = view.findViewById(R.id.backBtn);

        // Set up back button
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Load user data
        loadUserData();

        return view;
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String documentId = currentUser.getEmail();
        if (documentId == null) {
            // Try to get Facebook ID from provider data
            for (UserInfo profile : currentUser.getProviderData()) {
                if (profile.getProviderId().equals("facebook.com")) {
                    documentId = profile.getUid() + "@facebook.com";
                    break;
                }
            }
        }

        // Create a final copy of documentId to use in lambda
        final String finalDocumentId = documentId;
        
        if (finalDocumentId != null) {
            db.collection("users").document(finalDocumentId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            usernameTV.setText(document.getString("username"));
                            emailTV.setText(finalDocumentId);
                            phoneTV.setText(document.getString("phoneNumber"));
                            locationTV.setText(document.getString("location"));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                    });
        }
    }
} 