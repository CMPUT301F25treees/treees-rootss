package com.example.myapplication.features.user;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class UWaitlistAdapter extends RecyclerView.Adapter<UWaitlistAdapter.ViewHolder> {

    public interface OnItemClickListener{
        void onEventClick(UserEvent event);
    }

    private List<UserEvent> events;
    private final OnItemClickListener listener;

    public UWaitlistAdapter(List<UserEvent> items, OnItemClickListener listener){
        if(items == null){
            events = new ArrayList<>();
        } else {
            events = items;
        }

        this.listener = listener;
    }

    public void setItems(List<UserEvent> newItems) {
        if (newItems == null) {
            events= new ArrayList<>();
        } else {
            events = newItems;
        }
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_waitlist_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserEvent event = events.get(position);

        holder.titleTextView.setText(event.getName());


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView locationTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            titleTextView = itemView.findViewById(R.id.tvEventTitle);
            locationTextView = itemView.findViewById(R.id.tvLocation);
        }
    }


}
