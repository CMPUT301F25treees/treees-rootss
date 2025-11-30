package com.example.myapplication.features.organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.features.user.UserEvent;

import java.util.List;

/**
 * This is an Adapter used by organizers to display a list of events in a selection dialog.
 * Each row shows an event name and a radio button, allowing the user to choose exactyl one
 * event.
 */
public class OEventSelectAdapter extends RecyclerView.Adapter<OEventSelectAdapter.ViewHolder> {

    /**
     * Callback interface used to notify when the user selects a different event
     */
    public interface OnEventClickListener {
        void onEventClicked(int position);
    }

    private final List<UserEvent> events;
    private int selectedPos = -1;
    private final OnEventClickListener listener;

    /**
     * Constructs a new event selection adapter
     *
     * @param events the list of events to display
     * @param initiallySelected the index of the initially selected event
     * @param listener callback triggered when the selected event changes
     */
    public OEventSelectAdapter(List<UserEvent> events,
                               int initiallySelected,
                               OnEventClickListener listener) {
        this.events = events;
        this.selectedPos = initiallySelected;
        this.listener = listener;
    }

    /**
     * Inflates the item layout and creates a new ViewHolder for an event row
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_select_event, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Binds an event item to the ViewHolder, updating the text and selection state
     *
     * @param holder the ViewHolder for this row
     * @param position the index of the event being displayed
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserEvent event = events.get(position);

        // Name fill
        holder.tvEventName.setText(
                event.getName() != null ? event.getName() : "(unnamed)"
        );

        // Radio selection
        holder.radioButton.setChecked(position == selectedPos);

        View.OnClickListener click = v -> {
            int oldPos = selectedPos;
            selectedPos = holder.getBindingAdapterPosition();

            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPos);

            if (listener != null) {
                listener.onEventClicked(selectedPos);
            }
        };

        holder.itemView.setOnClickListener(click);
        holder.radioButton.setOnClickListener(click);
    }

    /**
     * Returns the total number of events displayed in the list
     *
     * @return number of events
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Holds references to the views inside a single event row
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName;
        RadioButton radioButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            radioButton = itemView.findViewById(R.id.radioSelect);
        }
    }
}