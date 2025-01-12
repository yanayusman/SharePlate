package com.example.shareplate;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ActionsFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private ImageView notificationIV;
    private Button giveAwayFoodButton, giveAwayNonFoodButton, requestFood, requestNonFood, hostEvent;
    private String mParam1;
    private String mParam2;

    public ActionsFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static ActionsFragment newInstance(String param1, String param2) {
        ActionsFragment fragment = new ActionsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_actions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the Give Away Food button
        giveAwayFoodButton = view.findViewById(R.id.give_away_food_button);
        giveAwayNonFoodButton = view.findViewById(R.id.give_away_non_food_button);
        requestFood = view.findViewById(R.id.request_food_button);
        requestNonFood = view.findViewById(R.id.request_non_food_button);
        hostEvent = view.findViewById(R.id.host_activity_button);
        notificationIV = view.findViewById(R.id.menu_icon2);

        notificationIV.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new NotificationAll())
                    .addToBackStack(null)
                    .commit();
        });

        // Set click listener
        giveAwayFoodButton.setOnClickListener(v -> {
            // Navigate to DonateItemFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new DonateItemFragment())
                    .addToBackStack(null)
                    .commit();
        });

        giveAwayNonFoodButton.setOnClickListener(v -> {
            // Navigate to DonateNonFoodFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new DonateNonFoodFragment())
                    .addToBackStack(null)
                    .commit();
        });

        requestFood.setOnClickListener(v -> {
            // Navigate to RequestFoodFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new RequestFoodFragment())
                    .addToBackStack(null)
                    .commit();
        });

        requestNonFood.setOnClickListener(v -> {
            // Navigate to RequestNonFoodFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new RequestNonFoodFragment())
                    .addToBackStack(null)
                    .commit();
        });

        hostEvent.setOnClickListener(v -> {
            // Navigate to HostActivityFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HostActivityFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
}