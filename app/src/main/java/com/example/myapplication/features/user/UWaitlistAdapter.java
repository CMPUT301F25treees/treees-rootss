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

    /**
     * Listener interface for receiving callbacks when a waitlist event item is clicked
     */
    public interface OnItemClickListener{
        void onEventClick(UserEvent event);
    }

    private List<UserEvent> events;
    private final OnItemClickListener listener;

    /**
     * Creates a new adapter instance for displaying waitlisted events
     *
     * @param items initial list of waitlist events
     * @param listener Callback gets invoked when a waitlist event is clicked
     */
    public UWaitlistAdapter(List<UserEvent> items, OnItemClickListener listener){
        if(items == null){
            events = new ArrayList<>();
        } else {
            events = items;
        }

        this.listener = listener;
    }

    /**
     * Replaces the current event list with a new set of items and refreshes the RecyclerView
     *
     * @param newItems the new list of events
     */
    public void setItems(List<UserEvent> newItems) {
        if (newItems == null) {
            events= new ArrayList<>();
        } else {
            events = newItems;
        }
        notifyDataSetChanged();
    }


    /**
     * Inflates the waitlist event item layout and creates a ViewHolder to represent it
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return a new ViewHolder containing the inflated item view
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_waitlist_event, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds event data to the ViewHolder at the given adapter position, formats the draw date and sets up
     * the click listener for the item
     *
     * @param holder the ViewHolder to update
     * @param position the position of the event
     */
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

    /**
     * Returns the number of waitlist events currently stored in the adapter
     *
     * @return the total number of items
     */
    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    /**
     * ViewHolder that stores references to UI components for a single waitlist event item,
     * holds the event title and formatted draw date text views
     */
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
