package com.example.shareplate;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationHelper {
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private final LocationResultListener listener;

    public interface LocationResultListener {
        void onLocationResult(Location location);
        void onLocationError(String error);
    }

    public LocationHelper(Context context, LocationResultListener listener) {
        this.context = context;
        this.listener = listener;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        createLocationCallback();
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d("LocationHelper", "Location received: " + location.getLatitude() + ", " + location.getLongitude());

                    // Check if it's the default Mountain View location
                    if (Math.abs(location.getLatitude() - 37.4219983) < 0.0001
                            && Math.abs(location.getLongitude() - (-122.084)) < 0.0001) {
                        listener.onLocationError("Default location detected");
                        return;
                    }

                    if (location.getAccuracy() <= 100) { // Only accept locations with accuracy better than 100 meters
                        listener.onLocationResult(location);
                    } else {
                        listener.onLocationError("Location accuracy too low: " + location.getAccuracy() + " meters");
                    }
                } else {
                    listener.onLocationError("Location is null");
                }
            }
        };
    }

    public void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listener.onLocationError("Location permission not granted");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(5000)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdates(5)  // Try up to 5 times to get a good location
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
        } catch (Exception e) {
            listener.onLocationError("Error requesting location updates: " + e.getMessage());
        }
    }

    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}