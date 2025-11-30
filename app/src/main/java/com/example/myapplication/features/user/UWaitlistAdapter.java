package com.example.myapplication.features.user;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter used to display events that the user is waitlisted for
 *
 * his adapter binds each event's title and selection (draw) date to the item
 * layout and forwards click events to a registered listener so the host fragment
 * can navigate to the event detail page.
 */
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

        long selectionDate = event.getSelectionDateMillis();
        Date date = new Date(selectionDate);
        SimpleDateFormat formattedDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String dateText = "Draw Date: " + formattedDate.format(date);

        holder.titleTextView.setText(event.getName());
        holder.dateTextView.setText(dateText);


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
        TextView dateTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            titleTextView = itemView.findViewById(R.id.tvEventTitle);
            dateTextView = itemView.findViewById(R.id.tvDate);
        }
    }


}
