package com.example.shareplate;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RateUIPage extends Dialog {

    private static final String TAG = "RateUIPage";
    private float userRate = 0;
    private final String userEmail;
    private final Context context;
    
    public RateUIPage(@NonNull Context context, String email) {
        super(context);
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }
        this.userEmail = email;
        this.context = context;
        Log.d(TAG, "RateUIPage created with email: " + email);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_rate_u_i_page);

        final AppCompatButton rateNowBtn = findViewById(R.id.rateNowBtn);
        final AppCompatButton laterBtn = findViewById(R.id.laterBtn);
        final RatingBar ratingBar = findViewById(R.id.ratingBar);
        final ImageView ratingImage = findViewById(R.id.ratingImage);

        rateNowBtn.setOnClickListener(v -> {
            try {
                Log.d(TAG, "Rate Now clicked with rating: " + userRate + " for user: " + userEmail);
                
                // Validate rating
                if (userRate == 0) {
                    Toast.makeText(context, "Please select a rating first", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Disable button to prevent multiple clicks
                rateNowBtn.setEnabled(false);

                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Log.e(TAG, "Current user is null");
                    Toast.makeText(context, "Please sign in to rate", Toast.LENGTH_SHORT).show();
                    rateNowBtn.setEnabled(true);
                    return;
                }

                // Create rating data
                Map<String, Object> ratingData = new HashMap<>();
                ratingData.put("rating", userRate);
                ratingData.put("timestamp", FieldValue.serverTimestamp());
                ratingData.put("raterEmail", currentUser.getEmail());

                // Submit rating to Firestore
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userEmail) // This should now never be null
                    .collection("UIRate")
                    .add(ratingData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Rating submitted successfully");
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(context, "Thank you for rating!", Toast.LENGTH_SHORT).show();
                            rateNowBtn.setEnabled(true);
                            dismiss();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error submitting rating", e);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(context, "Failed to submit rating. Please try again.", 
                                    Toast.LENGTH_SHORT).show();
                            rateNowBtn.setEnabled(true);
                        });
                    });

            } catch (Exception e) {
                Log.e(TAG, "Error in rate now button", e);
                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                rateNowBtn.setEnabled(true);
            }
        });

        laterBtn.setOnClickListener(v -> {
            try {
                dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing dialog", e);
            }
        });

        ratingBar.setOnRatingBarChangeListener((ratingBar1, rating, fromUser) -> {
            userRate = rating;
            Log.d(TAG, "Rating changed to: " + rating);
            updateRatingImage(rating, ratingImage);
        });
    }

    private void updateRatingImage(float rating, ImageView ratingImage) {
        try {
            if (rating <= 1) {
                ratingImage.setImageResource(R.drawable.one_star);
            } else if (rating <= 2) {
                ratingImage.setImageResource(R.drawable.two_star);
            } else if (rating <= 3) {
                ratingImage.setImageResource(R.drawable.three_star);
            } else if (rating <= 4) {
                ratingImage.setImageResource(R.drawable.four_star);
            } else {
                ratingImage.setImageResource(R.drawable.five_star);
            }
            animateImage(ratingImage);
        } catch (Exception e) {
            Log.e(TAG, "Error updating rating image", e);
        }
    }

    private void animateImage(ImageView ratingImage) {
        try {
            ScaleAnimation scaleAnimation = new ScaleAnimation(0, 1f, 0, 1f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnimation.setFillAfter(true);
            scaleAnimation.setDuration(200);
            ratingImage.startAnimation(scaleAnimation);
        } catch (Exception e) {
            Log.e(TAG, "Error animating image", e);
        }
    }
}