package com.example.myapplication.features.user;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.features.user.UPastEventItem;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying a list of past events for the user.
 * <p>
 * Binds {@link UPastEventItem} instances to {@code item_user_past_event} views,
 * formatting title, price, date, and status, and applying a status-specific
 * background colour to the status card.
 */
public class UPastEventsAdapter
        extends RecyclerView.Adapter<UPastEventsAdapter.PastEventViewHolder> {

    /**
     * Backing list of past event items displayed by this adapter.
     */
    private final List<UPastEventItem> items = new ArrayList<>();

    /**
     * Replaces the current list of items with a new collection and refreshes the UI.
     *
     * @param newItems the new list of {@link UPastEventItem} instances to display
     */
    public void setItems(List<UPastEventItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    /**
     * Inflates the item layout and creates a new {@link PastEventViewHolder}.
     *
     * @param parent   the parent ViewGroup into which the new view will be added
     * @param viewType the view type of the new view (unused as there is only one type)
     * @return a new {@link PastEventViewHolder} instance
     */
    @NonNull
    @Override
    public PastEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_past_event, parent, false);
        return new PastEventViewHolder(v);
    }

    /**
     * Binds the data from the {@link UPastEventItem} at the given position to the provided holder.
     * <p>
     * Sets title, price, date, and status text, and adjusts the status card background colour
     * based on the status value (e.g., Accepted, Invited, other).
     *
     * @param holder   the {@link PastEventViewHolder} to bind data to
     * @param position the position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull PastEventViewHolder holder, int position) {
        UPastEventItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvPrice.setText(item.getPriceDisplay());
        holder.tvDate.setText("Date: " + item.getDate());

        String status = item.getStatus();
        holder.tvStatus.setText(status);

        Context ctx = holder.itemView.getContext();
        if ("Accepted".equals(status)) {
            holder.cardStatus.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.selected_green));
        } else if ("Invited".equals(status)) {
            holder.cardStatus.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.waiting_orange));
        } else {
            holder.cardStatus.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.not_selected_red));
        }
    }

    /**
     * Returns the total number of items currently managed by this adapter.
     *
     * @return the item count
     */
    @Override
    public int getItemCount() { return items.size(); }

    /**
     * ViewHolder representing a single past event row in the RecyclerView.
     * <p>
     * Holds references to the event title, price, date, status text views,
     * and the status card used for colour-coding the event status.
     */
    static class PastEventViewHolder extends RecyclerView.ViewHolder {

        /** TextView displaying the event title. */
        TextView tvTitle;

        /** TextView displaying the formatted event price. */
        TextView tvPrice;

        /** TextView displaying the formatted event date. */
        TextView tvDate;

        /** TextView displaying the status label for the event. */
        TextView tvStatus;

        /** Card view used to visually indicate the event status by colour. */
        com.google.android.material.card.MaterialCardView cardStatus;

        /**
         * Creates a new {@code PastEventViewHolder} and binds view references.
         *
         * @param itemView the inflated item view for a single past event row
         */
        PastEventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cardStatus = itemView.findViewById(R.id.cardStatus);
        }
    }
}
