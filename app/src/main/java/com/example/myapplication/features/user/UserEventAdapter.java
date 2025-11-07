package com.example.myapplication.features.user;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * This class handles displaying the events properly in the view on the homepage
 *
 * Events are formatted and provide navigation functionality to a specified events
 * detail view.
 */
public class UserEventAdapter extends RecyclerView.Adapter<UserEventAdapter.EventViewHolder> {

    private final List<UserEvent> original = new ArrayList<>();
    private final List<UserEvent> visible  = new ArrayList<>();

    public interface OnEventClickListener {   // (you likely made this public already)
        void onEventClick(UserEvent event);
    }

    private OnEventClickListener listener;

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    /**
     * Submits the new list of events to the adapter
     *
     * @param events list of UserEvent objects
     */
    public void submit(List<UserEvent> events) {
        original.clear();
        visible.clear();
        if (events != null) {
            original.addAll(events);
            visible.addAll(events);
        }
        notifyDataSetChanged();
    }

    /**
     * Filters the in-memory list using the provided query and updates the visible items.
     */
    public void filter(String query) {
        visible.clear();
        if (TextUtils.isEmpty(query)) {
            visible.addAll(original);
        } else {
            String lower = query.toLowerCase(Locale.getDefault());
            for (UserEvent event : original) {
                // ✅ null-safe lookups
                String name  = safeLower(event.getName());
                String loc   = safeLower(event.getLocation());
                String instr = safeLower(event.getInstructor());

                if (name.contains(lower) || loc.contains(lower) || instr.contains(lower)) {
                    visible.add(event);
                }
            }
        }
        notifyDataSetChanged();
    }

    // ✅ helper to avoid NPEs in filter
    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.getDefault());
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_event, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds the objects to teh UI Elements.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        UserEvent event = visible.get(position);

        holder.name.setText(event.getName());
        holder.price.setText(String.valueOf(event.getPrice()));
        // ✅ null-safe binds for UI labels
        holder.location.setText(event.getLocation() == null ? "" : event.getLocation());

        String instr = event.getInstructor();
        holder.instructor.setText(TextUtils.isEmpty(instr)
                ? "" : String.format(Locale.getDefault(), "With %s", instr));

        holder.timeRemaining.setText(formatTimeRemaining(event.getEndTimeMillis()));
        bindBannerImage(event, holder);

        // keep your click behavior (robust to dataset changes)
        holder.itemView.setOnClickListener(x -> {
            if (listener != null) {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < visible.size()) {
                    listener.onEventClick(visible.get(pos));
                }
            }
        });
    }

    /**
     * @return the number of visible events.
     */
    @Override
    public int getItemCount() {
        return visible.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final View banner;
        final ImageView bannerImage;
        final TextView timeRemaining;
        final TextView name;
        final TextView price;
        final TextView location;
        final TextView instructor;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            banner = itemView.findViewById(R.id.eventBanner);
            bannerImage = itemView.findViewById(R.id.ivBannerImage);
            timeRemaining = itemView.findViewById(R.id.tvTimeRemaining);
            name = itemView.findViewById(R.id.tvEventName);
            price = itemView.findViewById(R.id.tvPrice);
            location = itemView.findViewById(R.id.tvLocation);
            instructor = itemView.findViewById(R.id.tvInstructor);
        }
    }

    /**
     * Returns a friendly time-remaining string (e.g., “2d 3h left”) for the event card.
     */
    private String formatTimeRemaining(long endTimeMillis) {
        long now = System.currentTimeMillis();
        long diff = endTimeMillis - now;
        if (diff <= 0) return "Ended";

        long days = TimeUnit.MILLISECONDS.toDays(diff);
        diff -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        diff -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);

        if (days > 0)   return String.format(Locale.getDefault(), "%dd %dh left", days, hours);
        if (hours > 0)  return String.format(Locale.getDefault(), "%dh %dm left", hours, minutes);
        return String.format(Locale.getDefault(), "%dm left", Math.max(minutes, 1));
    }

    /**
     * Loads the event poster/image into the banner view, falling back to a gradient background.
     */
    private void bindBannerImage(UserEvent event, EventViewHolder holder) {
        if (holder.bannerImage == null) {
            return;
        }

        String imageUrl = !TextUtils.isEmpty(event.getImageUrl())
                ? event.getImageUrl()
                : event.getPosterUrl();

        if (!TextUtils.isEmpty(imageUrl)) {
            holder.bannerImage.setBackground(null);
            Glide.with(holder.bannerImage.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .into(holder.bannerImage);
        } else {
            Glide.with(holder.bannerImage.getContext()).clear(holder.bannerImage);
            holder.bannerImage.setImageDrawable(null);
            holder.bannerImage.setBackgroundResource(R.drawable.bg_login_gradient);
        }
    }
}
