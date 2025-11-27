package com.example.myapplication.features.user;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * This class is an adapter that helps display the notifications.
 */
public class UNotiAdapter extends FirestoreRecyclerAdapter<UNotiItem, UNotiAdapter.UNotiViewHolder> {

    public interface OnOptionClickListener {
        void onOptionClick(DocumentSnapshot snapshot);
    }
    private boolean showPersonalNoti = true;

    public void setShowPersonalNoti(boolean show) {
        this.showPersonalNoti = show;
        notifyDataSetChanged();
    }

    private final OnOptionClickListener optionListener;

    public UNotiAdapter(@NonNull FirestoreRecyclerOptions<UNotiItem> options) {
        this(options, snapshot -> {});
    }

    public UNotiAdapter(@NonNull FirestoreRecyclerOptions<UNotiItem> options,
                        @NonNull OnOptionClickListener optionListener) {
        super(options);
        this.optionListener = optionListener;
    }


    /**
     * Binds the UNotiItem to the UI Elements
     *
     * @param holder ViewHolder that holds the notification layout
     * @param position the position of teh item in the adapter
     * @param model the model object containing the data that should be used to populate the view.
     */
    @Override
    protected void onBindViewHolder(@NonNull UNotiViewHolder holder, int position, @NonNull UNotiItem model) {
        if (!showPersonalNoti && "custom".equalsIgnoreCase(model.getType())) {
            // Hide and collapse this row
            holder.itemView.setVisibility(View.GONE);
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            if (params instanceof RecyclerView.LayoutParams) {
                RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) params;
                lp.height = 0;
                holder.itemView.setLayoutParams(lp);
            }
            return;
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            if (params instanceof RecyclerView.LayoutParams) {
                RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) params;
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                holder.itemView.setLayoutParams(lp);
            }
        }

        holder.fromText.setText(model.getFrom());
        holder.messageText.setText(model.getMessage());
        holder.eventText.setText(model.getEvent());

        holder.optionButton.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && optionListener != null) {
                optionListener.onOptionClick(getSnapshots().getSnapshot(pos));
            }
        });
    }

    @NonNull
    @Override
    public UNotiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_u_onenotif, parent, false);
        return new UNotiViewHolder(view);
    }

    /**
     * This class represents a single notification item that gets shown in the view
     */
    static class UNotiViewHolder extends RecyclerView.ViewHolder {
        final TextView fromText, messageText, eventText;
        final MaterialButton optionButton, acceptButton, declineButton;

        UNotiViewHolder(@NonNull View itemView) {
            super(itemView);
            fromText = itemView.findViewById(R.id.from_text);
            messageText = itemView.findViewById(R.id.message_text);
            eventText = itemView.findViewById(R.id.event_text);
            optionButton = itemView.findViewById(R.id.option_button);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
        }
    }
}