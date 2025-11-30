package com.example.myapplication.features.admin;

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

/**
 * RecyclerView adapter for the admin "Browse Profiles" list.
 * <p>
 * This adapter renders {@link UserRow} items for regular users and
 * organizers, including an inline action for organizer demotion. It
 * is used by {@link AUsersFrag} as the view layer in the admin MVC
 * design, while {@link AdminUsersController} provides the underlying
 * data and filtering.
 * <p>
 */
public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.VH> {

    /**
     * Lightweight view model representing a user row in the admin
     * browse-users list.
     */
    public static class UserRow {
        public String id;
        public String name;
        public String email;
        public String role;
        public String avatarUrl;
    }

    /**
     * Callback invoked when a user row is tapped (open detail).
     */
    public interface OnUserClick   { void onUser(UserRow u); }

    /**
     * Callback invoked when the inline remove/demote control is tapped.
     */
    public interface OnRemoveClick { void onRemove(UserRow u); }

    private final List<UserRow> original = new ArrayList<>();
    private final List<UserRow> visible  = new ArrayList<>();
    private OnUserClick onClick;
    private OnRemoveClick onRemove;

    /**
     * Registers a callback to handle user row taps.
     * @param cb callback receiving the tapped {@link UserRow}
     */
    public void setOnUserClick(OnUserClick cb)   { this.onClick  = cb; }

    /**
     * Registers a callback to handle inline organizer removal/demotion.
     * @param cb callback receiving the targeted {@link UserRow}
     */
    public void setOnRemoveClick(OnRemoveClick cb){ this.onRemove = cb; }

    /**
     * Replaces the adapter dataset and refreshes the list.
     * @param rows new rows to display (null or empty clears the list)
     */
    public void submit(List<UserRow> rows){
        original.clear(); visible.clear();
        if (rows != null){ original.addAll(rows); visible.addAll(rows); }
        notifyDataSetChanged();
    }

    /**
     * Filters the visible rows by name, email, or role (case-insensitive).
     * @param q query text; empty or null shows all rows
     */
    public void filter(String q){
        visible.clear();
        if (TextUtils.isEmpty(q)) {
            visible.addAll(original);
        } else {
            String lower = q.toLowerCase(Locale.getDefault());
            for (UserRow u : original) {
                if (safeLower(u.name).contains(lower)
                        || safeLower(u.email).contains(lower)
                        || safeLower(u.role).contains(lower)) {
                    visible.add(u);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Null-safe lowercase helper for filtering.
     * @param s source string (may be null)
     * @return lowercased value or empty string if null
     */
    private static String safeLower(String s){
        return s == null ? "" : s.toLowerCase(Locale.getDefault());
    }

    /**
     * Creates a new {@link VH} for a user row.
     * @param parent parent view group
     * @param viewType view type (unused)
     * @return a new {@link VH}
     */
    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new VH(row);
    }

    /**
     * Binds a {@link UserRow} to the given {@link VH}.
     * @param h holder to bind
     * @param position adapter position
     */
    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        UserRow u = visible.get(position);

        h.name.setText(u.name);
        h.email.setText(u.email);
        h.role.setText(cap(u.role));

        if (!TextUtils.isEmpty(u.avatarUrl)) {
            Glide.with(h.avatar.getContext()).load(u.avatarUrl).into(h.avatar);
        } else {
            h.avatar.setImageResource(R.mipmap.ic_launcher_round);
        }

        h.itemView.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (onClick != null && p != RecyclerView.NO_POSITION && p < visible.size()) {
                onClick.onUser(visible.get(p));
            }
        });

        boolean isOrganizer = "organizer".equalsIgnoreCase(u.role);
        h.btnRemove.setVisibility(isOrganizer ? View.VISIBLE : View.GONE);
        h.btnRemove.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (onRemove != null && p != RecyclerView.NO_POSITION && p < visible.size()) {
                onRemove.onRemove(visible.get(p));
            }
        });
    }

    /**
     * @return number of visible rows after filtering
     */
    @Override public int getItemCount() { return visible.size(); }

    /**
     * ViewHolder for a user row (avatar, metadata, and optional inline remove control).
     */
    static class VH extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name, email, role, btnRemove;

        /**
         * Constructs a holder bound to the provided item view.
         * @param itemView row root view
         */
        VH(@NonNull View itemView) {
            super(itemView);
            avatar    = itemView.findViewById(R.id.ivAvatar);
            name      = itemView.findViewById(R.id.tvName);
            email     = itemView.findViewById(R.id.tvEmail);
            role      = itemView.findViewById(R.id.tvRole);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }

    /**
     * Capitalizes the first character of a non-empty string.
     * @param s input string (may be null/empty)
     * @return capitalized string or empty string if input is null/empty
     */
    private static String cap(String s){
        if (TextUtils.isEmpty(s)) return "";
        return s.substring(0,1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }
}
