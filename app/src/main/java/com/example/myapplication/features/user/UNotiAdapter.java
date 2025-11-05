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

public class UNotiAdapter extends FirestoreRecyclerAdapter<UNotiItem, UNotiAdapter.UNotiViewHolder> {

    public interface OnOptionClickListener {
        void onOptionClick(DocumentSnapshot snapshot);
    }

    private final OnOptionClickListener listener;

    public UNotiAdapter(@NonNull FirestoreRecyclerOptions<UNotiItem> options) {
        this(options, snapshot -> { /* no-op */ });
    }

    public UNotiAdapter(@NonNull FirestoreRecyclerOptions<UNotiItem> options,
                        @NonNull OnOptionClickListener listener) {
        super(options);
        this.listener = listener;
    }

    @Override
    protected void onBindViewHolder(@NonNull UNotiViewHolder holder, int position, @NonNull UNotiItem model) {
        holder.fromText.setText(model.getFrom());
        holder.messageText.setText(model.getMessage());
        holder.eventText.setText(model.getEvent());

        holder.optionButton.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return; // view not bound anymore
            listener.onOptionClick(getSnapshots().getSnapshot(pos));
        });
    }

    @NonNull
    @Override
    public UNotiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_u_onenotif, parent, false);
        return new UNotiViewHolder(view);
    }

    static class UNotiViewHolder extends RecyclerView.ViewHolder {
        final TextView fromText, messageText, eventText;
        final MaterialButton optionButton;

        UNotiViewHolder(@NonNull View itemView) {
            super(itemView);
            fromText = itemView.findViewById(R.id.from_text);
            messageText = itemView.findViewById(R.id.message_text);
            eventText = itemView.findViewById(R.id.event_text);
            optionButton = itemView.findViewById(R.id.option_button);
        }
    }
}
