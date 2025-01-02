package com.example.shareplate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// This class is responsible for binding the notification data to the views in each item of the RecyclerView.
// It takes a list of Notification objects and binds each notification’s data (like the title, message, etc.)
// to the appropriate UI components (TextViews, ImageViews) in the item_notification.xml layout.

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> notificationList;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notificationList, OnNotificationClickListener listener) {
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        holder.title.setText(notification.getTitle());
        holder.message.setText(notification.getMessage());
        holder.timestamp.setText(notification.getTimestamp());
//        holder.icon.setImageResource(notification.getIconResId());

        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, timestamp;
        ImageView icon;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notificationTitle);
            message = itemView.findViewById(R.id.notificationMessage);
            timestamp = itemView.findViewById(R.id.notificationTimestamp);
            icon = itemView.findViewById(R.id.notificationIcon);
        }
    }
}