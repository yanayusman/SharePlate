package com.example.shareplate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class UserHistoryFragment extends Fragment {
    private static final String ARG_TYPE = "type";
    private static final String ARG_DONATION_ITEM = "donation_item";
    private static final String ARG_REQUEST_ITEM = "request_item";
    private static final String ARG_EVENT_ITEM = "event_item";
    private String type;
    private LinearLayout historyContainer;
    private TextView titleText, emptyStateText;
    private ImageView backButton;
    private FirebaseFirestore db;

    public static UserHistoryFragment newInstance(String type) {
        UserHistoryFragment fragment = new UserHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = getArguments().getString(ARG_TYPE);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        historyContainer = view.findViewById(R.id.history_container);
        titleText = view.findViewById(R.id.title_text);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        backButton = view.findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        setupTitle();
        loadHistory();
    }

    private void setupTitle() {
        switch (type) {
            case "donated":
                titleText.setText("Donation History");
                emptyStateText.setText("No donations yet");
                break;
            case "requested":
                titleText.setText("Request History");
                emptyStateText.setText("No requests yet");
                break;
            case "campaigns":
                titleText.setText("Campaigns History");
                emptyStateText.setText("No campaigns joined yet");
                break;
            case "volunteering":
                titleText.setText("Volunteering History");
                emptyStateText.setText("No volunteering activities yet");
                break;
        }
    }

    private void loadHistory() {
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String collection = getCollectionName();

        db.collection(collection)
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    historyContainer.removeAllViews();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        addHistoryItem(document);
                    }
                })
                .addOnFailureListener(e -> showEmptyState());
    }

    private String getCollectionName() {
        switch (type) {
            case "donated":
                return "allDonationItems";
            case "requested":
                return "foodRequest";
            case "campaigns":
            case "volunteering":
                return "events";
            default:
                return "";
        }
    }

    private void addHistoryItem(QueryDocumentSnapshot document) {
        View itemView = LayoutInflater.from(getContext())
                .inflate(R.layout.history_item, historyContainer, false);

        TextView nameText = itemView.findViewById(R.id.item_name);
        TextView dateText = itemView.findViewById(R.id.item_date);
        TextView statusText = itemView.findViewById(R.id.item_status);

        nameText.setText(document.getString("name"));
        dateText.setText(document.getString("pickupTime")); // or relevant date field
        statusText.setText(document.getString("status"));

        itemView.setOnClickListener(v -> {
            // Navigate to detail view based on type
            navigateToDetail(document);
        });

        historyContainer.addView(itemView);
    }

    private void navigateToDetail(QueryDocumentSnapshot document) {
        Fragment detailFragment = null;
        Bundle args = new Bundle();

        switch (type) {
            case "donated":
                detailFragment = new FoodItemDetailFragment();
                args.putSerializable(ARG_DONATION_ITEM, document.toObject(DonationItem.class));
                break;
            case "requested":
                detailFragment = new RequestItemDetailFragment();
                args.putSerializable(ARG_REQUEST_ITEM, document.toObject(DonationItem.class));
                break;
            case "campaigns":
            case "volunteering":
                detailFragment = new CommunityDetailsFragment();
                args.putSerializable(ARG_EVENT_ITEM, document.toObject(Event.class));
                break;
        }

        if (detailFragment != null) {
            detailFragment.setArguments(args);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void showEmptyState() {
        historyContainer.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
    }
} 