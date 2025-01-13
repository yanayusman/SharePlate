package com.example.shareplate;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

public class PlaceValidator {
    private final String apiKey;

    public PlaceValidator(String apiKey) {
        this.apiKey = apiKey;
    }

    public interface PlaceValidationCallback {
        void onSuccess(double[] coordinates);
        void onFailure(String error);
    }

    public void validatePlace(String place, PlaceValidationCallback callback) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" +
                place.replace(" ", "+") +
                "&key=" + apiKey;

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Failed to validate place: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseData = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseData);

                    if (jsonObject.getJSONArray("results").length() > 0) {
                        JSONObject location = jsonObject.getJSONArray("results")
                                .getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONObject("location");

                        double[] coordinates = new double[]{
                                location.getDouble("lat"),
                                location.getDouble("lng")
                        };
                        callback.onSuccess(coordinates);
                    } else {
                        callback.onFailure("Place not recognized: " + place);
                    }
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }
}