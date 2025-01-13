package com.example.shareplate;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

public class DistanceCalculator {
    private final String apiKey;
    private final PlaceValidator placeValidator;

    public DistanceCalculator(String apiKey) {
        this.apiKey = apiKey;
        this.placeValidator = new PlaceValidator(apiKey);
    }

    public interface DistanceCalculationCallback {
        void onSuccess(double distanceInKm);
        void onFailure(String error);
    }

    public void calculateDistance(String origin, String destination, DistanceCalculationCallback callback) {
        placeValidator.validatePlace(origin, new PlaceValidator.PlaceValidationCallback() {
            @Override
            public void onSuccess(double[] originCoords) {
                placeValidator.validatePlace(destination, new PlaceValidator.PlaceValidationCallback() {
                    @Override
                    public void onSuccess(double[] destinationCoords) {
                        OkHttpClient client = new OkHttpClient();
                        String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" +
                                originCoords[0] + "," + originCoords[1] +
                                "&destinations=" + destinationCoords[0] + "," + destinationCoords[1] +
                                "&key=" + apiKey;

                        Request request = new Request.Builder().url(url).build();
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                callback.onFailure("Failed to calculate distance: " + e.getMessage());
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                try {
                                    String responseData = response.body().string();
                                    JSONObject jsonObject = new JSONObject(responseData);

                                    if (jsonObject.getJSONArray("rows").length() > 0) {
                                        JSONObject elements = jsonObject.getJSONArray("rows")
                                                .getJSONObject(0)
                                                .getJSONArray("elements")
                                                .getJSONObject(0);

                                        if (elements.has("distance")) {
                                            // Extract numeric distance in meters
                                            double distanceInMeters = elements.getJSONObject("distance").getDouble("value");
                                            // Convert meters to kilometers
                                            double distanceInKilometers = distanceInMeters / 1000.0;
                                            callback.onSuccess(distanceInKilometers);
                                            return;
                                        }
                                    }
                                    callback.onFailure("Distance not found");
                                } catch (Exception e) {
                                    callback.onFailure("Error parsing distance response: " + e.getMessage());
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure("Failed to validate destination: " + error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure("Failed to validate origin: " + error);
            }
        });
    }
}