package com.example.myapplication.features.admin;

import android.text.TextUtils;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.VH> {

    public static class UserRow {
        public String id;
        public String name;
        public String email;
        public String role;
        public String avatarUrl;
    }

    public interface OnUserClick { void onUser(UserRow u); }

    private final List<UserRow> original = new ArrayList<>();
    private final List<UserRow> visible  = new ArrayList<>();
    private OnUserClick onClick;

    public void setOnUserClick(OnUserClick cb){ this.onClick = cb; }

    public void submit(List<UserRow> rows){
        original.clear(); visible.clear();
        if (rows != null){ original.addAll(rows); visible.addAll(rows); }
        notifyDataSetChanged();
    }

    public void filter(String q){
        visible.clear();
        if (TextUtils.isEmpty(q)) {
            visible.addAll(original);
        } else {
            String lower = q.toLowerCase(Locale.getDefault());
            for (UserRow u : original) {
                String n = safeLower(u.name), e = safeLower(u.email), r = safeLower(u.role);
                if (n.contains(lower) || e.contains(lower) || r.contains(lower)) visible.add(u);
            }
        }
        notifyDataSetChanged();
    }

    private static String safeLower(String s){ return s == null ? "" : s.toLowerCase(Locale.getDefault()); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View row = LayoutInflater.from(p.getContext()).inflate(R.layout.item_admin_user, p, false);
        return new VH(row);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        UserRow u = visible.get(pos);
        h.name.setText(u.name);
        h.email.setText(u.email);
        h.role.setText(cap(u.role));
        if (!TextUtils.isEmpty(u.avatarUrl)) Glide.with(h.avatar.getContext()).load(u.avatarUrl).into(h.avatar);
        h.itemView.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (onClick != null && p != RecyclerView.NO_POSITION && p < visible.size()) onClick.onUser(visible.get(p));
        });
    }

    @Override public int getItemCount(){ return visible.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name, email, role;
        VH(@NonNull View itemView){
            super(itemView);
            avatar = itemView.findViewById(R.id.ivAvatar);
            name   = itemView.findViewById(R.id.tvName);
            email  = itemView.findViewById(R.id.tvEmail);
            role   = itemView.findViewById(R.id.tvRole);
        }
    }

    private static String cap(String s) {
        if (TextUtils.isEmpty(s)) return "";
        return s.substring(0,1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }
}
