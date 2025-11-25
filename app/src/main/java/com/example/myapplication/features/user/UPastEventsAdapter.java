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

public class UPastEventsAdapter
        extends RecyclerView.Adapter<UPastEventsAdapter.PastEventViewHolder> {

    private final List<UPastEventItem> items = new ArrayList<>();

    public void setItems(List<UPastEventItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PastEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_past_event, parent, false);
        return new PastEventViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PastEventViewHolder holder, int position) {
        UPastEventItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvPrice.setText(item.getPriceDisplay());
        holder.tvDate.setText("Date: " + item.getDate());

        String status = item.getStatus();
        holder.tvStatus.setText(status);

        Context ctx = holder.itemView.getContext();
        if ("Selected".equals(status)) {
            holder.cardStatus.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.selected_green));
        } else if ("Waiting".equals(status)) {
            holder.cardStatus.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.waiting_orange));
        }else if ("Invited".equals(status)) {
            holder.cardStatus.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.waiting_orange));
        } else {
            holder.cardStatus.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.not_selected_red));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class PastEventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPrice, tvDate, tvStatus;
        com.google.android.material.card.MaterialCardView cardStatus;

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
