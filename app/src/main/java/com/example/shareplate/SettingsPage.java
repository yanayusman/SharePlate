package com.example.shareplate;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings_page);

        // Find the Rate App button
        LinearLayout rateAppButton = findViewById(R.id.rate_app_button);

        // Set click listener for Rate App button
//        rateAppButton.setOnClickListener(v -> {
//            // Show the rating dialog
//            RateUIPage rateUIPage = new RateUIPage(SettingsPage.this);
//            rateUIPage.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
//            rateUIPage.setCancelable(false);
//            rateUIPage.show();
//        });
    }
}