package com.example.shareplate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.os.Handler;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.shareplate.ProfilePage;

public class HomePageActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    private boolean isFragmentTransactionInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Prevent multiple rapid transactions
            if (isFragmentTransactionInProgress) {
                return false;
            }

            int itemId = item.getItemId();
            Fragment fragment = null;

            if (itemId == R.id.navigation_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.navigation_community) {
                fragment = new CommunityAllFragment();
            } else if (itemId == R.id.navigation_actions) {
                fragment = new ActionsFragment();
            } else if (itemId == R.id.navigation_profile) {
                fragment = new ProfilePage();
            }

            if (fragment != null) {
                replaceFragment(fragment);
                return true;
            }
            return false;
        });

        // Load the HomeFragment initially
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        }
    }

    private void replaceFragment(Fragment fragment) {
        isFragmentTransactionInProgress = true;
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
        
        // Reset the flag after a short delay to allow the transaction to complete
        new Handler().postDelayed(() -> {
            isFragmentTransactionInProgress = false;
        }, 300); // 300ms should be enough for most fragment transitions
    }
}