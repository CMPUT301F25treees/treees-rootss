package com.example.myapplication.features.organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter class for displaying a list of participant names (either from the waiting
 * list or final list) in the organizer event screen.
 *
 * <p>This adapter binds a list of names to simple text-based list items
 * defined in {@code item_waitlist_name.xml}. Each entry represents a user.</p>
 */
public class OEventListAdapter extends RecyclerView.Adapter<OEventListAdapter.NameVH> {

    /**
     * This interface is  a listener for a long-press action on the names in the list
     */
    public interface OnItemLongClickListener {
        /**
         * This gets called when a list item is long-pressed.
         *
         * @param position this is the adapter position of the item that was pressed
         */
        void onItemLongLCick(int position);
    }

    private OnItemLongClickListener longClickListener;

    /**
     * Sets up a callback listener for the long-press event
     *
     * @param listener the listener to be assigned
     */
    public void setOnItemLongClickListener(OnItemLongClickListener listener){
        this.longClickListener = listener;
    }


    /** The current list of participant names to display. */
    private final List<String> names = new ArrayList<>();

    /**
     * Replaces the current list of names and refreshes the adapter.
     *
     * @param names the new list of participant names
     */
    public void setNames(List<String> names) {
        List<String> old = new ArrayList<>(this.names);
        this.names.clear();
        this.names.addAll(names);
        notifyDataSetChanged();
    }

    /**
     * Inflates the item layout and creates a new {@link NameVH} (ViewHolder).
     *
     * @param parent   the parent {@link ViewGroup}
     * @param viewType the type of view (unused here)
     * @return a new {@link NameVH} instance
     */
    @NonNull
    @Override
    public NameVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waitlist_name, parent, false);
        return new NameVH(view);
    }

    /**
     * Binds a name to the provided {@link NameVH}.
     * Attaches a long press listener if it has been set.
     *
     * @param holder   the {@link NameVH} containing the views
     * @param position the current item index
     */
    @Override
    public void onBindViewHolder(@NonNull NameVH holder, int position) {
        holder.nameText.setText(names.get(position));

        holder.itemView.setOnLongClickListener(v ->{
            if(longClickListener != null){
                int position1 = holder.getAdapterPosition();
                if (position1 != RecyclerView.NO_POSITION){
                    longClickListener.onItemLongLCick(position1);
                }
            }
            return true;
        });
    }

    /**
     * @return the total number of names in the list
     */
    @Override
    public int getItemCount() {
        return names.size();
    }

    /**
     * ViewHolder class that holds a single TextView for displaying a name.
     */
    static class NameVH extends RecyclerView.ViewHolder {

        /** The TextView showing the participant's name. */
        final TextView nameText;

        /**
         * Constructs a new {@link NameVH} with the given item view.
         *
         * @param itemView the inflated item view for a list entry
         */
        NameVH(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
        }
    }

    /**
     * Removes a name at the given adapter position and updates teh RecyclerView.
     *
     * @param position this is the index of the item to remove.
     */
    public void removeAt(int position){
        if (position < 0 || position >= names.size()) {
            return;
        }

        names.remove(position);
        notifyItemRemoved(position);
    }
}
