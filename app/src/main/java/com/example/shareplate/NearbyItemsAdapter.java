package com.example.shareplate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class NearbyItemsAdapter extends RecyclerView.Adapter<NearbyItemsAdapter.ViewHolder> {
    private final List<DonationItem> items;

    public NearbyItemsAdapter(List<DonationItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DonationItem item = items.get(position);
        holder.titleText.setText(item.getName());
        holder.locationText.setText(item.getLocation());
        holder.typeText.setText(item.getDonateType());

        // Load image if available
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleText;
        TextView typeText;
        TextView locationText;

        ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.item_image);
            titleText = view.findViewById(R.id.item_title);
            typeText = view.findViewById(R.id.item_type);
            locationText = view.findViewById(R.id.item_location);
        }
    }
} 