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

public class OFinalListAdapter extends RecyclerView.Adapter<OFinalListAdapter.NameVH> {

    private final List<String> names = new ArrayList<>();

    public void setNames(List<String> names) {
        List<String> old = new ArrayList<>(this.names);
        this.names.clear();
        this.names.addAll(names);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NameVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_finallist_name, parent, false);
        return new NameVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NameVH holder, int position) {
        holder.nameText.setText(names.get(position));
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    static class NameVH extends RecyclerView.ViewHolder {
        final TextView nameText;
        NameVH(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
        }
    }
}
