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
    private static final int TRANSACTION_DEBOUNCE_TIME = 300; // milliseconds
    private Handler handler = new Handler();
    private Runnable pendingRunnable;

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
                final Fragment finalFragment = fragment;
                // Post the fragment transaction to the main thread
                handler.post(() -> replaceFragment(finalFragment));
                return true;
            }
            return false;
        });

        // Load the HomeFragment initially
        if (savedInstanceState == null) {
            handler.post(() -> replaceFragment(new HomeFragment()));
        }
    }

    private void replaceFragment(Fragment fragment) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        try {
            isFragmentTransactionInProgress = true;
            FragmentManager fragmentManager = getSupportFragmentManager();
            
            // Clear any pending fragment transactions
            fragmentManager.executePendingTransactions();
            
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commitAllowingStateLoss();
            
            // Reset the flag after a delay
            if (pendingRunnable != null) {
                handler.removeCallbacks(pendingRunnable);
            }
            
            pendingRunnable = () -> isFragmentTransactionInProgress = false;
            handler.postDelayed(pendingRunnable, TRANSACTION_DEBOUNCE_TIME);
            
        } catch (Exception e) {
            isFragmentTransactionInProgress = false;
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}