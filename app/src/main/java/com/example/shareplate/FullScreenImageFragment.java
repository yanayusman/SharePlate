package com.example.shareplate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

public class FullScreenImageFragment extends Fragment {
    private static final String ARG_IMAGE_URL = "image_url";
    private static final String ARG_IMAGE_RESOURCE = "image_resource";

    public static FullScreenImageFragment newInstance(String imageUrl, int imageResource) {
        FullScreenImageFragment fragment = new FullScreenImageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URL, imageUrl);
        args.putInt(ARG_IMAGE_RESOURCE, imageResource);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_full_screen_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView fullscreenImage = view.findViewById(R.id.fullscreen_image);
        ImageView closeButton = view.findViewById(R.id.close_button);

        if (getArguments() != null) {
            String imageUrl = getArguments().getString(ARG_IMAGE_URL);
            int imageResource = getArguments().getInt(ARG_IMAGE_RESOURCE);

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .error(imageResource)
                        .into(fullscreenImage);
            } else {
                fullscreenImage.setImageResource(imageResource);
            }
        }

        closeButton.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }
}