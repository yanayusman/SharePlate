package com.example.shareplate;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
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

    private float userRate = 0;
    private String userEmail;
    public RateUIPage(@NonNull Context context, String email) {
        super(context);
        this.userEmail = email;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_rate_u_i_page);

        final AppCompatButton rateNowBtn = findViewById(R.id.rateNowBtn);
        final AppCompatButton laterBtn = findViewById(R.id.laterBtn);
        final RatingBar ratingBar = findViewById(R.id.ratingBar);
        final ImageView ratingImage = findViewById(R.id.ratingImage);

        rateNowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the current user
                FirebaseAuth auth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = auth.getCurrentUser();

                if (currentUser != null) {

                    // Access Firestore
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    // Create a map to store the rating data
                    Map<String, Object> ratingData = new HashMap<>();
                    ratingData.put("rating", userRate);
                    ratingData.put("timestamp", FieldValue.serverTimestamp());

                    // Add to the UIRate subcollection of the current user's document
                    db.collection("users")
                            .document(userEmail) // Assuming the document ID is the user's email
                            .collection("UIRate")
                            .add(ratingData)
                            .addOnSuccessListener(documentReference -> {
                                // Successfully stored the rating
                                Toast.makeText(getContext(), "Rating submitted successfully!", Toast.LENGTH_SHORT).show();
                                dismiss();
                            })
                            .addOnFailureListener(e -> {
                                // Failed to store the rating
                                Log.e("FirestoreError", "Error submitting rating", e);
                                Toast.makeText(getContext(), "Failed to submit rating.", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        laterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // hide rating dialog
                dismiss();
            }
        });

        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {

                if(rating <= 1) {
                    ratingImage.setImageResource(R.drawable.one_star);
                }
                else if(rating <= 2) {
                    ratingImage.setImageResource(R.drawable.two_star);
                }
                else if(rating <= 3) {
                    ratingImage.setImageResource(R.drawable.three_star);
                }
                else if(rating <= 4) {
                    ratingImage.setImageResource(R.drawable.four_star);
                }
                else {
                    ratingImage.setImageResource(R.drawable.five_star);
                }

                // animate emoji image
                animateImage(ratingImage);

                // selected rating by user
                userRate = rating;
            }
        });
    }

    private void animateImage(ImageView ratingImage) {

        ScaleAnimation scaleAnimation = new ScaleAnimation(0, 1f, 0, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        scaleAnimation.setFillAfter(true);
        scaleAnimation.setDuration(200);
        ratingImage.startAnimation(scaleAnimation);
    }
}