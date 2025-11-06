package com.example.myapplication.features.user;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class UserEventAdapter extends RecyclerView.Adapter<UserEventAdapter.EventViewHolder> {

    private final List<UserEvent> original = new ArrayList<>();
    private final List<UserEvent> visible = new ArrayList<>();

    public interface OnEventClickListener {
        void onEventClick(UserEvent event);
    }

    private OnEventClickListener listener;

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }
    public void submit(List<UserEvent> events) {
        original.clear();
        visible.clear();
        if (events != null) {
            original.addAll(events);
            visible.addAll(events);
        }
        notifyDataSetChanged();
    }

    public void filter(String query) {
        visible.clear();
        if (TextUtils.isEmpty(query)) {
            visible.addAll(original);
        } else {
            String lower = query.toLowerCase(Locale.getDefault());
            for (UserEvent event : original) {
                if (event.getName().toLowerCase(Locale.getDefault()).contains(lower)
                        || event.getLocation().toLowerCase(Locale.getDefault()).contains(lower)
                        || event.getInstructor().toLowerCase(Locale.getDefault()).contains(lower)) {
                    visible.add(event);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        UserEvent event = visible.get(position);
        holder.banner.setBackgroundColor(event.getBannerColor());
        holder.name.setText(event.getName());
        String priceText = event.getPriceDisplay();
        if (TextUtils.isEmpty(priceText)) {
            holder.price.setVisibility(View.GONE);
        } else {
            holder.price.setVisibility(View.VISIBLE);
            holder.price.setText(priceText);
        }
        holder.location.setText(event.getLocation());
        holder.instructor.setText(String.format(Locale.getDefault(), "With %s", event.getInstructor()));
        holder.timeRemaining.setText(formatTimeRemaining(event.getEndTimeMillis()));

        holder.itemView.setOnClickListener(x -> {
            if(listener != null) {
                listener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return visible.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final View banner;
        final TextView timeRemaining;
        final TextView name;
        final TextView price;
        final TextView location;
        final TextView instructor;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            banner = itemView.findViewById(R.id.eventBanner);
            timeRemaining = itemView.findViewById(R.id.tvTimeRemaining);
            name = itemView.findViewById(R.id.tvEventName);
            price = itemView.findViewById(R.id.tvPrice);
            location = itemView.findViewById(R.id.tvLocation);
            instructor = itemView.findViewById(R.id.tvInstructor);
        }
    }

    private String formatTimeRemaining(long endTimeMillis) {
        long now = System.currentTimeMillis();
        long diff = endTimeMillis - now;
        if (diff <= 0) {
            return "Ended";
        }

        long days = TimeUnit.MILLISECONDS.toDays(diff);
        diff -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        diff -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);

        if (days > 0) {
            return String.format(Locale.getDefault(), "%dd %dh left", days, hours);
        }
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm left", hours, minutes);
        }
        return String.format(Locale.getDefault(), "%dm left", Math.max(minutes, 1));
    }
}
