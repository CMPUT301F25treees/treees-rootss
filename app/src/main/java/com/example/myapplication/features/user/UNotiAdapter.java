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

    public interface OnLotteryActionListener {
        void onAccept(DocumentSnapshot snapshot, UNotiItem item);
        void onDecline(DocumentSnapshot snapshot, UNotiItem item);
    }

    private final OnOptionClickListener optionListener;
    private final OnLotteryActionListener lotteryListener;

    public UNotiAdapter(@NonNull FirestoreRecyclerOptions<UNotiItem> options) {
        this(options, snapshot -> {}, null);
    }

    public UNotiAdapter(@NonNull FirestoreRecyclerOptions<UNotiItem> options,
                        @NonNull OnOptionClickListener optionListener) {
        this(options, optionListener, null);
    }

    public UNotiAdapter(@NonNull FirestoreRecyclerOptions<UNotiItem> options,
                        @NonNull OnOptionClickListener optionListener,
                        OnLotteryActionListener lotteryListener) {
        super(options);
        this.optionListener = optionListener;
        this.lotteryListener = lotteryListener;
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
        holder.fromText.setText(model.getFrom());
        holder.messageText.setText(model.getMessage());
        holder.eventText.setText(model.getEvent());

        // Handle lottery win notifications with accept/decline buttons
        if (model.isLotteryWin() && model.isPending()) {
            holder.optionButton.setVisibility(View.GONE);
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.declineButton.setVisibility(View.VISIBLE);

            holder.acceptButton.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && lotteryListener != null) {
                    lotteryListener.onAccept(getSnapshots().getSnapshot(pos), model);
                }
            });

            holder.declineButton.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && lotteryListener != null) {
                    lotteryListener.onDecline(getSnapshots().getSnapshot(pos), model);
                }
            });
        }
        // Show status for already responded lottery wins
        else if (model.isLotteryWin() && (model.isAccepted() || model.isDeclined())) {
            holder.optionButton.setVisibility(View.VISIBLE);
            holder.acceptButton.setVisibility(View.GONE);
            holder.declineButton.setVisibility(View.GONE);

            String statusText = model.isAccepted() ? "✓ Accepted" : "✗ Declined";
            holder.optionButton.setText(statusText);
            holder.optionButton.setEnabled(false);
        }
        // Regular notifications or lottery lose
        else {
            holder.optionButton.setVisibility(View.VISIBLE);
            holder.acceptButton.setVisibility(View.GONE);
            holder.declineButton.setVisibility(View.GONE);

            holder.optionButton.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    optionListener.onOptionClick(getSnapshots().getSnapshot(pos));
                }
            });
        }
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