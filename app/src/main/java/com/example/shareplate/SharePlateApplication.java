package com.example.shareplate;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class SharePlateApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Populate initial data
        new DonationItemRepository();
    }
}
