package com.example.shareplate;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommunityAllFragment extends Fragment {

    private Toolbar toolbar;
    private ImageView searchIcon, notificationIV, backArrow, imgView;
    private Button interestedButton, notInterestedButton;
    private TextView joinPromptText;
    private LinearLayout eventGrid;
    private EditText searchEditText;
    private List<Event> allEvents = new ArrayList<>();
    private LinearLayout searchLayout;
    private View normalToolbarContent;
    private EventRepo eventRepo;
    private List<Event> volunteeringEvents = new ArrayList<>();
    private List<Event> campaignEvents = new ArrayList<>();

    private MaterialButton allEventsButton, volunteeringButton, campaignsButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community_all, container, false);

        eventRepo = new EventRepo();
        fetchEventsFromDatabase();

        // Initialize views
        toolbar = view.findViewById(R.id.toolbar2);
        searchIcon = view.findViewById(R.id.search_icon2);
        allEventsButton = view.findViewById(R.id.allEventsButton);
        volunteeringButton = view.findViewById(R.id.volunteeringButton);
        campaignsButton = view.findViewById(R.id.campaignsButton);
        eventGrid = view.findViewById(R.id.event_grid);
        searchEditText = view.findViewById(R.id.search_edit_text2);
        searchLayout = view.findViewById(R.id.search_layout2);
        backArrow = view.findViewById(R.id.back_arrow2);
        normalToolbarContent = view.findViewById(R.id.normal_toolbar_content2);
        notificationIV = view.findViewById(R.id.menu_icon2);
        imgView = view.findViewById(R.id.imageView6);
        joinPromptText = view.findViewById(R.id.joinPromptTextView);
        interestedButton = view.findViewById(R.id.interestedButton);
        notInterestedButton = view.findViewById(R.id.notNowButton);

        respondCheck(view);

        notificationIV.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new NotificationAll())
                    .addToBackStack(null)
                    .commit();
        });

        // Set up search functionality
        searchIcon.setOnClickListener(v -> {
            searchLayout.setVisibility(View.VISIBLE);
            normalToolbarContent.setVisibility(View.GONE);
            searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        backArrow.setOnClickListener(v -> {
            searchLayout.setVisibility(View.GONE);
            normalToolbarContent.setVisibility(View.VISIBLE);
            searchEditText.setText("");
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterEvents(s.toString());
            }
        });

        allEventsButton.setBackgroundColor(getResources().getColor(R.color.button_green));
        allEventsButton.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.button_green)));
        allEventsButton.setTextColor(Color.WHITE);

        allEventsButton.setOnClickListener(v -> {
            updateFilterButtonStates(allEventsButton, volunteeringButton, campaignsButton);
            navigateToFragment(new CommunityAllFragment());
        });

        volunteeringButton.setOnClickListener(v -> {
            updateFilterButtonStates(volunteeringButton, allEventsButton, campaignsButton);
            navigateToFragment(new CommunityVolunteeringFragment());
        });

        campaignsButton.setOnClickListener(v -> {
            updateFilterButtonStates(campaignsButton, allEventsButton, volunteeringButton);
            navigateToFragment(new CommunityCampaignsFragment());
        });

        return view;
    }

    public void respondCheck(View view) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userEmail = currentUser.getEmail();

        db.collection("users")
                .document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String response = documentSnapshot.getString("volunteeringRespond");
                        if ("Yes".equals(response)) {
                            imgView.setVisibility(View.GONE);
                            joinPromptText.setVisibility(View.GONE);
                            interestedButton.setVisibility(View.GONE);
                            notInterestedButton.setVisibility(View.GONE);
                        } else {
                            // Show button if not responded
                            interestedButton.setVisibility(View.VISIBLE);
                            interestedButton.setOnClickListener(v -> interestedRespond(view));

                            notInterestedButton.setVisibility(View.VISIBLE);
                            notInterestedButton.setOnClickListener(v -> notInterestedRespond(view));
                        }
                    } else {
                        // Show button if document does not exist

                        interestedButton.setVisibility(View.VISIBLE);
                        interestedButton.setOnClickListener(v -> interestedRespond(view));

                        notInterestedButton.setVisibility(View.VISIBLE);
                        notInterestedButton.setOnClickListener(v -> notInterestedRespond(view));
                    }
                })
                .addOnFailureListener(e -> {
                    interestedButton.setVisibility(View.GONE); // Hide button if there's an error
                    Log.e("Firestore", "Error checking volunteering response: ", e);
                });
    }

    public void interestedRespond(View view){
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userEmail = currentUser.getEmail();

        Map<String, Object> data = new HashMap<>();
        data.put("volunteeringRespond", "Yes");

        db.collection("users")
                .document(userEmail)
                .set(data, SetOptions.merge()) // Merge to avoid overwriting existing data
                .addOnSuccessListener(unused -> {
                    // Hide the button on success
                    imgView.setVisibility(View.GONE);
                    joinPromptText.setVisibility(View.GONE);
                    interestedButton.setVisibility(View.GONE);
                    notInterestedButton.setVisibility(View.GONE);
                    Toast.makeText(view.getContext(), "Thank you for joining the community!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    imgView.setVisibility(View.VISIBLE);
                    joinPromptText.setVisibility(View.VISIBLE);
                    interestedButton.setVisibility(View.VISIBLE);
                    notInterestedButton.setVisibility(View.VISIBLE);
                    Toast.makeText(view.getContext(), "Failed to to join: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void notInterestedRespond(View view){
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userEmail = currentUser.getEmail();

        Map<String, Object> data = new HashMap<>();
        data.put("volunteeringRespond", "No");

        db.collection("users")
                .document(userEmail)
                .set(data, SetOptions.merge()) // Merge to avoid overwriting existing data
                .addOnSuccessListener(unused -> {
                    // Hide the button on success
                    imgView.setVisibility(View.GONE);
                    joinPromptText.setVisibility(View.GONE);
                    interestedButton.setVisibility(View.GONE);
                    notInterestedButton.setVisibility(View.GONE);
                    Toast.makeText(view.getContext(), "Thank you for joining the community!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    imgView.setVisibility(View.VISIBLE);
                    joinPromptText.setVisibility(View.VISIBLE);
                    interestedButton.setVisibility(View.VISIBLE);
                    notInterestedButton.setVisibility(View.VISIBLE);
                    Toast.makeText(view.getContext(), "Failed to to join: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToFragment(Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void fetchEventsFromDatabase() {
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        if (currentUserEmail == null || currentUserEmail.isEmpty()) {
            Toast.makeText(getContext(), "Error: No email found for the current user.", Toast.LENGTH_SHORT).show();
            return;
        }

        eventRepo.getAllEventItems(new EventRepo.OnEventItemsLoadedListener() {
            @Override
            public void onEventItemsLoaded(List<Event> items) {
                if (!isAdded() || getView() == null) {
                    Log.e("CommunityAllFragment", "Fragment not attached, skipping UI update.");
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    allEvents.clear();
                    allEvents.addAll(items);
                    for (Event item : items) {
                        addEventsView(item);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, currentUserEmail);
    }

    private void addEventsView(Event event) {
        if (!isAdded() || getView() == null) {
            Log.e("CommunityAllFragment", "Fragment not attached to UI, skipping event addition.");
            return;
        }

        View eventItem = getLayoutInflater().inflate(R.layout.event_item_view, eventGrid, false);

        ImageView eventImg = eventItem.findViewById(R.id.item_image);
        TextView eventName = eventItem.findViewById(R.id.item_name);
        TextView eventDate = eventItem.findViewById(R.id.item_date);
        TextView eventLocation = eventItem.findViewById(R.id.item_location);
        TextView eventDesc = eventItem.findViewById(R.id.item_desc);
        Button joinButton = eventItem.findViewById(R.id.BtnJoin);

        // Set event details
        eventImg.setImageResource(event.getImageResourceId());
        eventName.setText(event.getName());
        eventDate.setText("Date : " + (event.getDate() != null ? event.getDate() : "N/A"));
        eventLocation.setText("Location : " + (event.getLocation() != null ? event.getLocation() : "N/A"));
        eventDesc.setText("Description : " + (event.getDescription() != null ? event.getDescription() : "N/A"));

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String currentEmail = currentUser.getEmail();
            if (currentEmail != null) {
                // Fetch the user's joined events from Firestore
                firestore.collection("users")
                        .document(currentEmail)
                        .collection("joinedEvents")
                        .whereEqualTo("eventName", event.getName())
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                // User has already joined the event
                                joinButton.setVisibility(View.VISIBLE);
                                joinButton.setEnabled(false);
                                joinButton.setText("Joined!");
                                joinButton.setBackgroundColor(Color.LTGRAY);
                                joinButton.setTextColor(Color.WHITE);
                            } else {
                                // User has not joined the event
                                joinButton.setVisibility(View.VISIBLE);
                                joinButton.setEnabled(true);
                                joinButton.setText("Join Now!");
                                joinButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.button_green_dark));

                                joinButton.setOnClickListener(v -> {
                                    CommunityDetailsFragment detailFragment = CommunityDetailsFragment.newInstance(event);
                                    requireActivity().getSupportFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.fragment_container, detailFragment)
                                            .addToBackStack(null)
                                            .commit();
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ButtonsVisibility", "Failed to check joined events: " + e.getMessage());
                            joinButton.setVisibility(View.GONE);
                        });
            }
        }
        eventGrid.addView(eventItem);
    }

    private void filterEvents(String query) {
        eventGrid.removeAllViews();

        if (query.isEmpty()) {
            for (Event event : allEvents) {
                addEventsView(event);
            }
        } else {
            String lowercaseQuery = query.toLowerCase();
            List<Event> filteredEvents = allEvents.stream()
                    .filter(item ->
                            item.getName().toLowerCase().contains(lowercaseQuery) ||
                                    item.getDate().toLowerCase().contains(lowercaseQuery) ||
                                    item.getLocation().toLowerCase().contains(lowercaseQuery) ||
                                    item.getDescription().toLowerCase().contains(lowercaseQuery))
                    .collect(Collectors.toList());

            for (Event event : filteredEvents) {
                addEventsView(event);
            }
        }
    }

    private void updateFilterButtonStates(Button selectedButton, Button... otherButtons) {
        // Reset all buttons to unselected state
        allEventsButton.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.button_green)));
        volunteeringButton.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.button_green)));
        campaignsButton.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.button_green)));

        allEventsButton.setBackgroundColor(Color.TRANSPARENT);
        volunteeringButton.setBackgroundColor(Color.TRANSPARENT);
        campaignsButton.setBackgroundColor(Color.TRANSPARENT);

        allEventsButton.setTextColor(getResources().getColor(R.color.button_green));
        volunteeringButton.setTextColor(getResources().getColor(R.color.button_green));
        campaignsButton.setTextColor(getResources().getColor(R.color.button_green));
        
        selectedButton.setBackgroundColor(getResources().getColor(R.color.button_green));
        selectedButton.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.button_green)));
        selectedButton.setTextColor(Color.WHITE);

        for (Button button : otherButtons) {
            button.setBackgroundColor(Color.WHITE);
            button.setTextColor(Color.BLACK);
        }
    }
}
